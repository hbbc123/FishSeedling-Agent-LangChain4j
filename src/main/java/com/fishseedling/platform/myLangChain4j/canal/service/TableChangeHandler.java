package com.fishseedling.platform.myLangChain4j.canal.service;

import com.alibaba.otter.canal.protocol.CanalEntry;

import java.util.Map;
import java.util.Set;

/**
 * Canal 表变更处理器接口
 * 每种需要监听的表实现该接口，由 CanalConsumerService 自动发现和调度
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
public interface TableChangeHandler {

    /**
     * 返回该处理器监听的表名
     * 例如：tb_handbooks, tb_help_content
     */
    String getTableName();

    /**
     * 返回需要监听的字段集合（用于 UPDATE 时判断是否需要处理）
     * 返回空集合表示 UPDATE 时始终处理
     */
    Set<String> getWatchedFields();

    /**
     * 处理 INSERT 事件
     * @param rowChange 行变更数据
     */
    void handleInsert(CanalEntry.RowChange rowChange);

    /**
     * 处理 UPDATE 事件
     * @param rowChange  行变更数据
     * @param beforeData 变更前的字段值（key: 字段名, value: 字段值）
     * @param afterData  变更后的字段值
     */
    void handleUpdate(CanalEntry.RowChange rowChange, Map<String, Object> beforeData, Map<String, Object> afterData);

    /**
     * 处理 DELETE 事件
     * @param rowChange 行变更数据
     */
    void handleDelete(CanalEntry.RowChange rowChange);
}
