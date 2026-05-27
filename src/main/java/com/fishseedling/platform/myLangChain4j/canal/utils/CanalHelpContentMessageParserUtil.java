package com.fishseedling.platform.myLangChain4j.canal.utils;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.fishseedling.platform.entity.HelpContent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Canal 消息解析工具 - HelpContent 专用
 * 将 Canal RowChange 解析为 HelpContent 实体列表
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
public class CanalHelpContentMessageParserUtil {

    public static List<HelpContent> parseEntry(CanalEntry.RowChange rowChange, boolean isAfter) {
        return rowChange.getRowDatasList().stream()
                .map(rowData -> {
                    List<CanalEntry.Column> columns = isAfter
                            ? rowData.getAfterColumnsList()
                            : rowData.getBeforeColumnsList();
                    return parseToHelpContent(columns);
                })
                .collect(Collectors.toList());
    }

    private static HelpContent parseToHelpContent(List<CanalEntry.Column> columns) {
        HelpContent helpContent = new HelpContent();

        for (CanalEntry.Column column : columns) {
            String name = column.getName();
            String value = column.getValue();

            if (value == null) {
                continue;
            }

            switch (name) {
                case "id":
                    helpContent.setId(Long.parseLong(value));
                    break;
                case "category":
                    helpContent.setCategory(value);
                    break;
                case "title":
                    helpContent.setTitle(value);
                    break;
                case "content":
                    helpContent.setContent(value);
                    break;
                case "sort_order":
                    if (!value.isEmpty()) {
                        helpContent.setSortOrder(Integer.parseInt(value));
                    }
                    break;
                case "status":
                    helpContent.setStatus(value);
                    break;
                case "create_time":
                    helpContent.setCreateTime(parseDateTime(value));
                    break;
                case "update_time":
                    helpContent.setUpdateTime(parseDateTime(value));
                    break;
                default:
                    break;
            }
        }
        return helpContent;
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            String datetime = value.replace(" ", "T");
            if (datetime.contains(".")) {
                datetime = datetime.substring(0, datetime.indexOf("."));
            }
            return LocalDateTime.parse(datetime);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(value.replace(" ", "T"));
            } catch (Exception ex) {
                return null;
            }
        }
    }
}

