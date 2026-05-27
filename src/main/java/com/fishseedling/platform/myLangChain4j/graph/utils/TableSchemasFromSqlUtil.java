package com.fishseedling.platform.myLangChain4j.graph.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableSchemasFromSqlUtil {
    public static String extractTableSchemasFromSql(String sqlFileName) {
        try {
            InputStream inputStream = TableSchemasFromSqlUtil.class.getClassLoader()
                    .getResourceAsStream("documents/" + sqlFileName);

            if (inputStream == null) {
                throw new RuntimeException("文件不存在: documents/" + sqlFileName);
            }

            String sqlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            StringBuilder result = new StringBuilder();

            // 按 "CREATE TABLE" 分割
            String[] tableParts = sqlContent.split("CREATE TABLE");

            for (int i = 1; i < tableParts.length; i++) {
                String part = tableParts[i];

                // 提取表名
                Pattern tablePattern = Pattern.compile("`?(\\w+)`?\\s*\\(", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = tablePattern.matcher(part);

                if (!tableMatcher.find()) continue;

                String tableName = tableMatcher.group(1);

                // 提取表注释（如果有）
                String tableComment = extractTableComment(part);

                // 提取括号内的列定义
                int startIdx = part.indexOf('(');
                int endIdx = findMatchingBracket(part, startIdx);
                String tableBody = part.substring(startIdx + 1, endIdx);

                // 解析列定义
                List<ColumnInfo> columns = parseColumns(tableBody);

                // 生成输出格式
                result.append("Table: ").append(tableName);
                if (!tableComment.isEmpty()) {
                    result.append(" (").append(tableComment).append(")");
                }
                result.append("\n");
                result.append("Columns: ");

                List<String> columnNames = new ArrayList<>();
                Map<String, String> columnComments = new HashMap<>();

                for (ColumnInfo col : columns) {
                    columnNames.add(col.name);
                    if (col.comment != null && !col.comment.isEmpty()) {
                        columnComments.put(col.name, col.comment);
                    }
                }
                result.append(String.join(", ", columnNames));
                result.append("\n");

                // 生成字段说明
                result.append("Description: ");
                List<String> descParts = new ArrayList<>();
                for (ColumnInfo col : columns) {
                    if (col.comment != null && !col.comment.isEmpty()) {
                        descParts.add(col.name + "(" + col.comment + ")");
                    } else {
                        descParts.add(col.name);
                    }
                }
                result.append(String.join(", ", descParts));
                result.append("\n\n");
            }


            return result.toString();

        } catch (IOException e) {
            throw new RuntimeException("读取 SQL 文件失败: " + sqlFileName, e);
        }
    }

    /**
     * 提取表的注释
     */
    private static String extractTableComment(String tableDef) {
        Pattern commentPattern = Pattern.compile("COMMENT\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = commentPattern.matcher(tableDef);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 解析列定义
     */
    private static List<ColumnInfo> parseColumns(String tableBody) {
        List<ColumnInfo> columns = new ArrayList<>();

        // 按行分割
        String[] lines = tableBody.split("\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 跳过约束定义（PRIMARY KEY, FOREIGN KEY, KEY, INDEX, CONSTRAINT）
            String upperLine = line.toUpperCase();
            if (upperLine.startsWith("PRIMARY") ||
                    upperLine.startsWith("FOREIGN") ||
                    upperLine.startsWith("UNIQUE") ||
                    (upperLine.startsWith("KEY") && !upperLine.contains("COMMENT")) ||
                    upperLine.startsWith("INDEX") ||
                    upperLine.startsWith("CONSTRAINT")) {
                continue;
            }

            // 提取列定义：`column_name` type ... COMMENT 'comment'
            Pattern colPattern = Pattern.compile("`?(\\w+)`?\\s+(\\w+(?:\\([^)]+\\))?)\\s*(.*?)(?:,|$)", Pattern.CASE_INSENSITIVE);
            Matcher colMatcher = colPattern.matcher(line);

            if (colMatcher.find()) {
                String columnName = colMatcher.group(1);
                String columnType = colMatcher.group(2);
                String rest = colMatcher.group(3);

                // 提取 COMMENT
                String comment = "";
                Pattern commentPattern = Pattern.compile("COMMENT\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
                Matcher commentMatcher = commentPattern.matcher(rest);
                if (commentMatcher.find()) {
                    comment = commentMatcher.group(1);
                }

                columns.add(new ColumnInfo(columnName, columnType, comment));
            }
        }

        return columns;
    }

    /**
     * 找到匹配的右括号位置
     */
    private static int findMatchingBracket(String str, int startIdx) {
        int count = 1;
        for (int i = startIdx + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') count++;
            if (c == ')') count--;
            if (count == 0) return i;
        }
        return str.length() - 1;
    }

    /**
     * 列信息内部类
     */
    private static class ColumnInfo {
        String name;
        String type;
        String comment;

        ColumnInfo(String name, String type, String comment) {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }
    }
}
