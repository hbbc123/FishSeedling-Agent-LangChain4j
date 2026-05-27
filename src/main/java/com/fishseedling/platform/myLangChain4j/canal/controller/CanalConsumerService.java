package com.fishseedling.platform.myLangChain4j.canal.controller;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.fishseedling.platform.myLangChain4j.canal.service.TableChangeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Canal 消费者服务（可扩展架构）
 * 通过 TableChangeHandler 接口自动发现所有表变更处理器，
 * 动态订阅所有配置的表，并将事件分发给对应的处理器。
 *
 * 新增监听表只需：
 * 1. 实现 TableChangeHandler 接口
 * 2. 添加 @Component 注解
 * 无需修改本类。
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
@Slf4j
@Service
public class CanalConsumerService {

    private final CanalConnector canalConnector;
    private final Map<String, TableChangeHandler> handlerMap;
    private volatile boolean running = true;
    private ExecutorService executorService;

    /**
     * 构造函数，Spring 自动注入所有 TableChangeHandler 实现
     */
    public CanalConsumerService(CanalConnector canalConnector,
                                List<TableChangeHandler> handlers) {
        this.canalConnector = canalConnector;

        // 将 handlers 列表转为 Map<表名, 处理器>，方便快速查找
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        TableChangeHandler::getTableName,
                        h -> h,
                        (existing, replacement) -> {
                            log.warn("表 {} 存在多个处理器，使用后者", existing.getTableName());
                            return replacement;
                        },
                        LinkedHashMap::new
                ));

        log.info("CanalConsumerService 初始化完成，已注册 {} 个表处理器: {}",
                handlerMap.size(),
                handlerMap.keySet());
    }

    @PostConstruct
    public void start() {
        if (handlerMap.isEmpty()) {
            log.warn("没有注册任何 TableChangeHandler，Canal 消费者不会启动");
            return;
        }

        log.debug("=== 启动 Canal 消费者（可扩展架构）===");
        log.debug("已注册表处理器: " + handlerMap.keySet());

        new Thread(() -> {
            try {
                canalConnector.connect();
                log.debug("Canal 连接成功");

                canalConnector.rollback();

                // 动态构建订阅表达式：database\\.table1,database\\.table2,...
                String subscribeFilter = handlerMap.keySet().stream()
                        .map(tableName -> "fish_seedling_platform\\." + tableName)
                        .collect(Collectors.joining(","));
                log.debug("订阅过滤器: " + subscribeFilter);

                canalConnector.subscribe(subscribeFilter);
                log.debug("订阅成功");

                while (true) {
                    Message message = canalConnector.getWithoutAck(100, 1000L, TimeUnit.MILLISECONDS);

                    if (message.getId() != -1 && message.getEntries().size() > 0) {
                        log.debug("收到消息！batchId=" + message.getId());
                        processMessage(message);
                        canalConnector.ack(message.getId());
                    }
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                log.error("Canal 消费者异常", e);
            }
        }).start();
    }

    /**
     * 处理消息：遍历 entries，根据表名分发给对应的 Handler
     */
    private void processMessage(Message message) {
        log.debug("=== processMessage, entries数量: " + message.getEntries().size() + " ===");

        for (CanalEntry.Entry entry : message.getEntries()) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }

            try {
                String tableName = entry.getHeader().getTableName();
                TableChangeHandler handler = handlerMap.get(tableName);

                if (handler == null) {
                    log.debug("=== 未找到表 [" + tableName + "] 的处理器，跳过 ===");
                    continue;
                }

                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                CanalEntry.EventType eventType = rowChange.getEventType();
                log.debug("=== 表: " + tableName + ", 事件: " + eventType + " ===");

                dispatchEvent(handler, rowChange, eventType);

            } catch (Exception e) {
                System.err.println("=== 处理 ROWDATA 失败 ===");
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据事件类型分发给对应的 Handler 方法
     */
    private void dispatchEvent(TableChangeHandler handler, CanalEntry.RowChange rowChange, CanalEntry.EventType eventType) {
        switch (eventType) {
            case INSERT:
                handler.handleInsert(rowChange);
                break;

            case UPDATE:
                handleUpdateWithFieldCheck(handler, rowChange);
                break;

            case DELETE:
                handler.handleDelete(rowChange);
                break;

            default:
                log.debug("=== 忽略事件类型: " + eventType + " ===");
                break;
        }
    }

    /**
     * 处理 UPDATE 事件（含字段变更判断）
     */
    private void handleUpdateWithFieldCheck(TableChangeHandler handler, CanalEntry.RowChange rowChange) {
        Set<String> watchedFields = handler.getWatchedFields();

        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
            Map<String, Object> beforeData = extractColumnsToMap(rowData.getBeforeColumnsList());
            Map<String, Object> afterData = extractColumnsToMap(rowData.getAfterColumnsList());

            // 如果没有配置监听字段，则全部处理
            if (watchedFields == null || watchedFields.isEmpty()) {
                handler.handleUpdate(rowChange, beforeData, afterData);
                return;
            }

            // 判断关注的字段是否有变化
            if (hasWatchedFieldChanged(watchedFields, beforeData, afterData)) {
                log.debug("=== 关注的字段发生变化，触发同步 ===");
                handler.handleUpdate(rowChange, beforeData, afterData);
            } else {
                log.debug("=== 关注的字段未变化，跳过 ===");
            }
        }
    }

    /**
     * 判断关注的字段是否有变化
     */
    private boolean hasWatchedFieldChanged(Set<String> watchedFields,
                                           Map<String, Object> beforeData,
                                           Map<String, Object> afterData) {
        for (String field : watchedFields) {
            Object beforeValue = beforeData.get(field);
            Object afterValue = afterData.get(field);
            if (!Objects.equals(beforeValue, afterValue)) {
                log.debug(String.format("字段 [%s] 发生变化: %s -> %s", field, beforeValue, afterValue));
                return true;
            }
        }
        return false;
    }

    /**
     * 将 Canal Column 列表转为 Map（含类型转换）
     */
    private Map<String, Object> extractColumnsToMap(List<CanalEntry.Column> columns) {
        Map<String, Object> dataMap = new HashMap<>();
        for (CanalEntry.Column column : columns) {
            String name = column.getName();
            String value = column.getValue();
            String sqlType = String.valueOf(column.getSqlType());

            if (value == null || "null".equalsIgnoreCase(value)) {
                dataMap.put(name, null);
            } else if (sqlType.contains("INT") || sqlType.contains("TINY") || sqlType.contains("SMALL")) {
                dataMap.put(name, Integer.parseInt(value));
            } else if (sqlType.contains("BIGINT")) {
                dataMap.put(name, Long.parseLong(value));
            } else if (sqlType.contains("DOUBLE") || sqlType.contains("DECIMAL")) {
                dataMap.put(name, Double.parseDouble(value));
            } else {
                dataMap.put(name, value);
            }
        }
        return dataMap;
    }

    @PreDestroy
    public void destroy() {
        log.info("停止 Canal 消费者");
        running = false;
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("Canal 消费者已停止");
    }
}