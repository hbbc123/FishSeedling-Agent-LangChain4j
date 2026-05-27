package com.fishseedling.platform.myLangChain4j.canal.utils;


import com.alibaba.otter.canal.protocol.CanalEntry;
import com.fishseedling.platform.entity.Handbook;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Canal 消息解析工具
 */
@Slf4j
public class CanalHandBookMessageParserUtil {

    /**
     * 解析 RowChange 中的每一行数据
     *
     * @param rowChange 行变更数据
     * @param isAfter   是否解析 after 列（true: after, false: before）
     * @return 解析后的实体对象列表
     */
    public static List<Handbook> parseEntry(CanalEntry.RowChange rowChange, boolean isAfter) {
        return  rowChange.getRowDatasList().stream()
                .map(rowData -> {
                    List<CanalEntry.Column> columns = isAfter
                            ? rowData.getAfterColumnsList()
                            : rowData.getBeforeColumnsList();
                    return parseToHandbook(columns);
                })
                .collect(Collectors.toList());
    }

    /**
     * 将 Column 列表转换为 Handbook 实体
     */
    private static Handbook parseToHandbook(List<CanalEntry.Column> columns) {
        Handbook handbook = new Handbook();

        for (CanalEntry.Column column : columns) {
            String name = column.getName();
            String value = column.getValue();

            if (value == null) {
                continue;
            }

            switch (name) {
                case "id":
                    handbook.setId(Integer.parseInt(value));
                    break;
                case "fish_name":
                    handbook.setFishName(value);
                    break;
                case "fish_alias":
                    handbook.setFishAlias(value);
                    break;
                case "category":
                    handbook.setCategory(value);
                    break;
                case "cover_image":
                    handbook.setCoverImage(value);
                    break;
                case "brief_intro":
                    handbook.setBriefIntro(value);
                    break;
                case "difficulty":
                    handbook.setDifficulty(value);
                    break;
                case "content":
                    handbook.setContent(value);
                    break;
                case "is_published":
                    handbook.setIsPublished(Integer.parseInt(value));
                    break;
                case "sort_order":
                    if (value != null && !value.isEmpty()) {
                        handbook.setSortOrder(Integer.parseInt(value));
                    }
                    break;
                case "view_count":
                    if (value != null && !value.isEmpty()) {
                        handbook.setViewCount(Integer.parseInt(value));
                    }
                    break;
                case "create_time":
                    handbook.setCreateTime(parseDateTime(value));
                    break;
                case "update_time":
                    handbook.setUpdateTime(parseDateTime(value));
                    break;
                default:
                    // 忽略未知字段
                    break;
            }
        }
        log.debug("======查看触发类=====");
        log.debug(String.valueOf(handbook));
        return handbook;
    }

    /**
     * 解析日期时间字段
     * 支持格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-ddTHH:mm:ss
     */
    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            // 处理可能的时间格式
            String datetime = value.replace(" ", "T");
            // 去除毫秒部分（如果有）
            if (datetime.contains(".")) {
                datetime = datetime.substring(0, datetime.indexOf("."));
            }
            return LocalDateTime.parse(datetime);
        } catch (Exception e) {
            try {
                // 尝试另一种格式
                return LocalDateTime.parse(value.replace(" ", "T"));
            } catch (Exception ex) {
                return null;
            }
        }
    }

}