package com.fishseedling.platform.myLangChain4j.postgresql.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL 聊天消息持久化存储工具类
 * 不使用 Spring 管理，通过 Builder 模式手动构建
 *
 * 使用方式：
 * PostgresChatMessageStore store = PostgresChatMessageStore.builder()
 *     .dataSource(dataSource)
 *     .maxMessages(20)
 *     .autoCreateTable(true)
 *     .build();
 */
@Slf4j
public class PostgresChatMessageStore {

    // ==================== 常量定义 ====================
    private static final String TABLE_NAME = "chat_message_history";
    private static final long CACHE_EXPIRE_MS = 60000;
    private static final int DEFAULT_MAX_MESSAGES = 10;

    private static final String SQL_CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS chat_message_history (
            thread_id VARCHAR(255) NOT NULL PRIMARY KEY,
            messages_json TEXT NOT NULL,
            message_count INTEGER NOT NULL DEFAULT 0,
            max_messages INTEGER NOT NULL DEFAULT 10,
            version INTEGER NOT NULL DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """;

    private static final String SQL_CREATE_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_thread_id ON chat_message_history(thread_id);
        CREATE INDEX IF NOT EXISTS idx_updated_at ON chat_message_history(updated_at)
        """;

    private static final String SQL_UPSERT = """
        INSERT INTO chat_message_history (thread_id, messages_json, message_count, max_messages, version, updated_at)
        VALUES (?, ?::TEXT, ?, ?, 1, CURRENT_TIMESTAMP)
        ON CONFLICT (thread_id) DO UPDATE SET
            messages_json = EXCLUDED.messages_json,
            message_count = EXCLUDED.message_count,
            max_messages = EXCLUDED.max_messages,
            version = chat_message_history.version + 1,
            updated_at = CURRENT_TIMESTAMP
        """;

    private static final String SQL_SELECT_BY_THREAD_ID = """
        SELECT messages_json, max_messages FROM chat_message_history WHERE thread_id = ?
        """;

    private static final String SQL_DELETE_BY_THREAD_ID = """
        DELETE FROM chat_message_history WHERE thread_id = ?
        """;

    // ==================== 成员变量 ====================
    private final javax.sql.DataSource dataSource;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final int defaultMaxMessages;
    private final boolean autoCreateTable;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // ==================== 私有构造函数 ====================
    private PostgresChatMessageStore(Builder builder) {
        this.dataSource = builder.dataSource;
        this.jdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(this.dataSource);
        this.objectMapper = createObjectMapper();
        this.defaultMaxMessages = builder.maxMessages > 0 ? builder.maxMessages : DEFAULT_MAX_MESSAGES;
        this.autoCreateTable = builder.autoCreateTable;

        // 初始化表
        if (autoCreateTable) {
            initTable();
        }

        log.info("PostgresChatMessageStore 初始化完成，数据源: {}, 默认最大消息数: {}",
                dataSource.getClass().getSimpleName(), defaultMaxMessages);
    }

    // ==================== Builder 模式 ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private javax.sql.DataSource dataSource;
        private int maxMessages = DEFAULT_MAX_MESSAGES;
        private boolean autoCreateTable = true;

        /**
         * 设置数据源（必需）
         */
        public Builder dataSource(javax.sql.DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        /**
         * 设置默认最大消息条数
         */
        public Builder maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        /**
         * 设置是否自动创建表
         */
        public Builder autoCreateTable(boolean autoCreateTable) {
            this.autoCreateTable = autoCreateTable;
            return this;
        }

        /**
         * 构建 PostgresChatMessageStore 实例
         */
        public PostgresChatMessageStore build() {
            if (dataSource == null) {
                throw new IllegalArgumentException("dataSource 不能为空");
            }
            return new PostgresChatMessageStore(this);
        }
    }

    // ==================== 初始化方法 ====================

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private void initTable() {
        try {
            jdbcTemplate.execute(SQL_CREATE_TABLE);
            jdbcTemplate.execute(SQL_CREATE_INDEX);
            log.info("数据库表初始化成功: {}", TABLE_NAME);
        } catch (Exception e) {
            log.error("数据库表初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    // ==================== 核心业务方法 ====================

    /**
     * 添加单条消息
     */
    public void addMessage(String threadId, ChatMessage message, int maxMessages) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId 不能为空");
        }
        if (message == null) {
            throw new IllegalArgumentException("message 不能为空");
        }
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages 必须大于0");
        }
        addMessages(threadId, Collections.singletonList(message), maxMessages);
    }

    /**
     * 添加单条消息（使用默认最大消息数）
     */
    public void addMessage(String threadId, ChatMessage message) {
        addMessage(threadId, message, defaultMaxMessages);
    }

    /**
     * 批量添加消息
     */
    public void addMessages(String threadId, List<ChatMessage> newMessages, int maxMessages) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId 不能为空");
        }
        if (newMessages == null || newMessages.isEmpty()) {
            log.warn("newMessages 为空，跳过添加操作");
            return;
        }
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages 必须大于0");
        }

        log.debug("添加消息: threadId={}, 新增数量={}, maxMessages={}", threadId, newMessages.size(), maxMessages);

        try {
            List<ChatMessage> existingMessages = getExistingMessages(threadId);
            List<ChatMessage> allMessages = new ArrayList<>(existingMessages);
            allMessages.addAll(newMessages);
            List<ChatMessage> limitedMessages = applyMaxMessagesLimit(allMessages, maxMessages);
            saveToDatabase(threadId, limitedMessages, maxMessages);
            updateCache(threadId, limitedMessages);

            log.info("消息添加成功: threadId={}, 新增={}, 当前总消息数={}",
                    threadId, newMessages.size(), limitedMessages.size());
        } catch (Exception e) {
            log.error("添加消息失败: threadId={}", threadId, e);
            throw new RuntimeException("添加消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量添加消息（使用默认最大消息数）
     */
    public void addMessages(String threadId, List<ChatMessage> newMessages) {
        addMessages(threadId, newMessages, defaultMaxMessages);
    }

    /**
     * 根据线程ID删除整个会话
     */
    public boolean deleteByThreadId(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId 不能为空");
        }

        log.info("删除会话: threadId={}", threadId);

        try {
            int affectedRows = jdbcTemplate.update(SQL_DELETE_BY_THREAD_ID, threadId);
            cache.remove(threadId);
            boolean deleted = affectedRows > 0;
            log.debug("删除{}: threadId={}, 影响行数={}", deleted ? "成功" : "失败", threadId, affectedRows);
            return deleted;
        } catch (Exception e) {
            log.error("删除会话失败: threadId={}", threadId, e);
            throw new RuntimeException("删除会话失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取所有消息列表
     */
    public List<ChatMessage> getMessages(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId 不能为空");
        }

        log.debug("获取消息列表: threadId={}", threadId);

        CacheEntry cached = cache.get(threadId);
        if (cached != null && !isCacheExpired(cached)) {
            log.debug("命中缓存: threadId={}, 消息数={}", threadId, cached.getMessages().size());
            return new ArrayList<>(cached.getMessages());
        }

        List<ChatMessage> messages = getExistingMessages(threadId);

        if (!messages.isEmpty()) {
            updateCache(threadId, messages);
        }

        log.debug("从数据库获取: threadId={}, 消息数={}", threadId, messages.size());
        return messages;
    }

    /**
     * 获取最后一条消息
     */
    public Optional<ChatMessage> getLastMessage(String threadId) {
        List<ChatMessage> messages = getMessages(threadId);
        if (messages.isEmpty()) {
            log.debug("没有找到消息: threadId={}", threadId);
            return Optional.empty();
        }
        ChatMessage lastMessage = messages.get(messages.size() - 1);
        log.debug("获取最后一条消息: threadId={}, type={}", threadId, lastMessage.type());
        return Optional.of(lastMessage);
    }

    /**
     * 更新最大消息条数配置
     */
    public void updateMaxMessages(String threadId, int newMaxMessages) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId 不能为空");
        }
        if (newMaxMessages <= 0) {
            throw new IllegalArgumentException("newMaxMessages 必须大于0");
        }

        log.info("更新最大消息条数: threadId={}, newMaxMessages={}", threadId, newMaxMessages);

        try {
            List<ChatMessage> currentMessages = getExistingMessages(threadId);
            List<ChatMessage> limitedMessages = applyMaxMessagesLimit(currentMessages, newMaxMessages);
            saveToDatabase(threadId, limitedMessages, newMaxMessages);
            updateCache(threadId, limitedMessages);

            log.info("更新max_messages成功: threadId={}, 原消息数={}, 现消息数={}",
                    threadId, currentMessages.size(), limitedMessages.size());
        } catch (Exception e) {
            log.error("更新max_messages失败: threadId={}", threadId, e);
            throw new RuntimeException("更新max_messages失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清空所有消息（保留会话配置）
     */
    public void clearMessages(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId 不能为空");
        }

        log.info("清空消息: threadId={}", threadId);

        try {
            int maxMessages = getMaxMessages(threadId);
            saveToDatabase(threadId, Collections.emptyList(), maxMessages);
            updateCache(threadId, Collections.emptyList());
            log.debug("清空消息成功: threadId={}", threadId);
        } catch (Exception e) {
            log.error("清空消息失败: threadId={}", threadId, e);
            throw new RuntimeException("清空消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查会话是否存在
     */
    public boolean exists(String threadId) {
        String sql = "SELECT COUNT(1) FROM chat_message_history WHERE thread_id = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, threadId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查会话存在性失败: threadId={}", threadId, e);
            return false;
        }
    }

    /**
     * 获取会话统计信息
     */
    public SessionStatistics getStatistics(String threadId) {
        String sql = "SELECT message_count, max_messages, version, created_at, updated_at FROM chat_message_history WHERE thread_id = ?";

        try {
            return jdbcTemplate.query(sql, rs -> {
                if (rs.next()) {
                    SessionStatistics stats = new SessionStatistics();
                    stats.setThreadId(threadId);
                    stats.setMessageCount(rs.getInt("message_count"));
                    stats.setMaxMessages(rs.getInt("max_messages"));
                    stats.setVersion(rs.getInt("version"));
                    stats.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    stats.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return stats;
                }
                return null;
            }, threadId);
        } catch (Exception e) {
            log.error("获取统计信息失败: threadId={}", threadId, e);
            return null;
        }
    }

    /**
     * 清理过期的缓存条目
     */
    public void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        int beforeSize = cache.size();
        cache.entrySet().removeIf(entry -> (now - entry.getValue().getTimestamp()) > CACHE_EXPIRE_MS);
        log.debug("清理过期缓存: 清理前={}, 清理后={}", beforeSize, cache.size());
    }

    // ==================== 私有方法 ====================

    private List<ChatMessage> getExistingMessages(String threadId) {
        try {
            return jdbcTemplate.query(SQL_SELECT_BY_THREAD_ID, rs -> {
                if (rs.next()) {
                    String messagesJson = rs.getString("messages_json");
                    if (messagesJson != null && !messagesJson.isEmpty()) {
                        return ChatMessageDeserializer.messagesFromJson(messagesJson);
                    }
                }
                return new ArrayList<>();
            }, threadId);
        } catch (Exception e) {
            log.error("获取现有消息失败: threadId={}", threadId, e);
            return new ArrayList<>();
        }
    }

    private List<ChatMessage> applyMaxMessagesLimit(List<ChatMessage> messages, int maxMessages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        if (messages.size() <= maxMessages) {
            return new ArrayList<>(messages);
        }
        int excessCount = messages.size() - maxMessages;
        List<ChatMessage> limitedMessages = new ArrayList<>(messages.subList(excessCount, messages.size()));

        log.debug("消息条数限制: 原始={}, 限制后={}, 删除最旧{}条",
                messages.size(), limitedMessages.size(), excessCount);

        return limitedMessages;
    }

    private void saveToDatabase(String threadId, List<ChatMessage> messages, int maxMessages) {
        try {
            String messagesJson = ChatMessageSerializer.messagesToJson(messages);

            int affectedRows = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(SQL_UPSERT);
                ps.setString(1, threadId);
                ps.setString(2, messagesJson);
                ps.setInt(3, messages.size());
                ps.setInt(4, maxMessages);
                return ps;
            });

            log.debug("保存到数据库成功: threadId={}, 消息数={}, 影响行数={}",
                    threadId, messages.size(), affectedRows);
        } catch (Exception e) {
            log.error("保存到数据库失败: threadId={}", threadId, e);
            throw new RuntimeException("保存到数据库失败: " + e.getMessage(), e);
        }
    }

    private int getMaxMessages(String threadId) {
        try {
            Integer maxMessages = jdbcTemplate.queryForObject(
                    "SELECT max_messages FROM chat_message_history WHERE thread_id = ?",
                    Integer.class, threadId);
            return maxMessages != null ? maxMessages : defaultMaxMessages;
        } catch (Exception e) {
            log.debug("获取max_messages失败，使用默认值{}: threadId={}", defaultMaxMessages, threadId);
            return defaultMaxMessages;
        }
    }

    private void updateCache(String threadId, List<ChatMessage> messages) {
        cache.put(threadId, new CacheEntry(new ArrayList<>(messages), System.currentTimeMillis()));
        log.trace("更新缓存: threadId={}, 消息数={}", threadId, messages.size());
    }

    private boolean isCacheExpired(CacheEntry entry) {
        return (System.currentTimeMillis() - entry.getTimestamp()) > CACHE_EXPIRE_MS;
    }

    // ==================== 内部类 ====================

    private static class CacheEntry {
        private final List<ChatMessage> messages;
        private final long timestamp;

        CacheEntry(List<ChatMessage> messages, long timestamp) {
            this.messages = messages;
            this.timestamp = timestamp;
        }

        public List<ChatMessage> getMessages() { return messages; }
        public long getTimestamp() { return timestamp; }
    }

    public static class SessionStatistics {
        private String threadId;
        private int messageCount;
        private int maxMessages;
        private int version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public String getThreadId() { return threadId; }
        public void setThreadId(String threadId) { this.threadId = threadId; }
        public int getMessageCount() { return messageCount; }
        public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        @Override
        public String toString() {
            return String.format("SessionStatistics{threadId='%s', messageCount=%d, maxMessages=%d, version=%d}",
                    threadId, messageCount, maxMessages, version);
        }
    }
}