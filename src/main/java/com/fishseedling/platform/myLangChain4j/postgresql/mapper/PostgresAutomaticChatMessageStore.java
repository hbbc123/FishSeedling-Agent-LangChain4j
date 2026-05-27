package com.fishseedling.platform.myLangChain4j.postgresql.mapper;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import dev.langchain4j.data.message.ChatMessageSerializer;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL 实现的 ChatMemoryStore
 * 支持 Builder 模式，从外部传入 DataSource
 * 全量更新模式：每次 updateMessages 都会覆盖整个会话的消息记录
 */
@Slf4j
public class PostgresAutomaticChatMessageStore implements ChatMemoryStore {


    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // 默认窗口大小（20条）
    private static final int DEFAULT_MAX_MESSAGES = 20;

    // SQL：插入或更新（全量覆盖 messages_json）
    private static final String SQL_UPSERT = """
            INSERT INTO chat_message_history (thread_id, messages_json, message_count, max_messages, version, updated_at)
            VALUES (?, ?::TEXT, ?, ?, 1, CURRENT_TIMESTAMP)
            ON CONFLICT (thread_id) DO UPDATE SET
                messages_json = EXCLUDED.messages_json,
                message_count = EXCLUDED.message_count,
                version = chat_message_history.version + 1,
                updated_at = CURRENT_TIMESTAMP
            """;

    // SQL：查询消息
    private static final String SQL_SELECT = """
            SELECT messages_json FROM chat_message_history WHERE thread_id = ?
            """;

    // SQL：删除
    private static final String SQL_DELETE = """
            DELETE FROM chat_message_history WHERE thread_id = ?
            """;

    // 私有构造器，使用 Builder
    private PostgresAutomaticChatMessageStore(Builder builder) {
        this.dataSource = builder.dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.objectMapper = createObjectMapper();

        // 自动初始化表
        if (builder.initTables) {
            initTables();
        }
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 支持多态类型（处理 ChatMessage 的子类）
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return mapper;
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DataSource dataSource;
        private boolean initTables = true;  // 默认自动建表

        /**
         * 设置数据源（必需）
         */
        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        /**
         * 是否自动建表，默认 true
         */
        public Builder initTables(boolean initTables) {
            this.initTables = initTables;
            return this;
        }

        public PostgresAutomaticChatMessageStore build() {
            if (dataSource == null) {
                throw new IllegalArgumentException("DataSource cannot be null");
            }
            return new PostgresAutomaticChatMessageStore(this);
        }
    }

    // ==================== 表初始化 ====================

    private void initTables() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS chat_message_history (
                thread_id VARCHAR(255) NOT NULL PRIMARY KEY,
                messages_json TEXT NOT NULL,
                message_count INTEGER NOT NULL DEFAULT 0,
                max_messages INTEGER NOT NULL DEFAULT 20,
                version INTEGER NOT NULL DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createIndexSql = """
            CREATE INDEX IF NOT EXISTS idx_thread_id ON chat_message_history(thread_id);
            CREATE INDEX IF NOT EXISTS idx_updated_at ON chat_message_history(updated_at)
            """;

        try (Connection conn = DataSourceUtils.getConnection(dataSource);
             PreparedStatement psTable = conn.prepareStatement(createTableSql);
             PreparedStatement psIndex = conn.prepareStatement(createIndexSql)) {
            psTable.execute();
            psIndex.execute();
            log.info("ChatMessageHistory 表初始化完成");
        } catch (SQLException e) {
            log.error("初始化表失败", e);
            throw new RuntimeException("初始化 ChatMessageHistory 表失败", e);
        }
    }

    // ==================== ChatMemoryStore 接口实现 ====================

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String threadId = memoryId.toString();
        log.debug("获取消息: threadId={}", threadId);

        try {
            String json = jdbcTemplate.queryForObject(SQL_SELECT, (rs, rowNum) -> rs.getString("messages_json"), threadId);
            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            }
            return deserializeMessages(json);
        } catch (EmptyResultDataAccessException e) {
            log.debug("未找到会话: threadId={}", threadId);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取消息失败: threadId={}", threadId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String threadId = memoryId.toString();
        log.debug("更新消息: threadId={}, 消息数={}", threadId, messages == null ? 0 : messages.size());

        if (messages == null) {
            messages = new ArrayList<>();
        }

        try {
            String json = serializeMessages(messages);
            int messageCount = messages.size();

            // 全量更新：覆盖 messages_json 字段
            jdbcTemplate.update(SQL_UPSERT, threadId, json, messageCount, DEFAULT_MAX_MESSAGES);
            log.debug("消息更新成功: threadId={}, 消息数={}", threadId, messageCount);
        } catch (Exception e) {
            log.error("更新消息失败: threadId={}", threadId, e);
            throw new RuntimeException("更新消息历史失败", e);
        }
    }

    public void  addMessages(Object memoryId,ChatMessage chatMessage){
        String threadId = memoryId.toString();
        List<ChatMessage> messages = this.getMessages(threadId);
        messages.add(chatMessage);
        this.updateMessages(threadId,messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String threadId = memoryId.toString();
        log.debug("删除消息: threadId=");
        log.debug("删除消息: threadId={}", threadId);

        jdbcTemplate.update(SQL_DELETE, threadId);
    }

    public ChatMessage deleteLastMessages(Object memoryId) {
        List<ChatMessage> messages = getMessages(memoryId);
        ChatMessage remove = messages.remove(messages.size() - 1);
        this.updateMessages(memoryId,messages);
        return remove;
    }


    // ==================== 序列化/反序列化 ====================

    public String serializeMessages(List<ChatMessage> messages) {
        try {
            // ChatMessageSerializer 内部已经处理了多态类型
            return ChatMessageSerializer.messagesToJson(messages);
        } catch (Exception e) {
            log.error("序列化消息失败", e);
            throw new RuntimeException("序列化消息失败", e);
        }
    }

    public List<ChatMessage> deserializeMessages(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            // ChatMessageDeserializer 可以正确反序列化所有消息类型
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (Exception e) {
            log.error("反序列化消息失败", e);
            throw new RuntimeException("反序列化消息失败", e);
        }
    }

    // ==================== 额外辅助方法 ====================

    /**
     * 获取最后一条消息
     */
    public ChatMessage getLastMessage(String threadId) {
        List<ChatMessage> messages = getMessages(threadId);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    /**
     * 获取最后一条用户消息
     */
    public dev.langchain4j.data.message.UserMessage getLastUserMessage(String threadId) {
        List<ChatMessage> messages = getMessages(threadId);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg instanceof dev.langchain4j.data.message.UserMessage) {
                return (dev.langchain4j.data.message.UserMessage) msg;
            }
        }
        return null;
    }

    /**
     * 获取最后一条 AI 消息
     */
    public dev.langchain4j.data.message.AiMessage getLastAiMessage(String threadId) {
        List<ChatMessage> messages = getMessages(threadId);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg instanceof dev.langchain4j.data.message.AiMessage) {
                return (dev.langchain4j.data.message.AiMessage) msg;
            }
        }
        return null;
    }

    /**
     * 获取最后一条系统消息
     */
    public dev.langchain4j.data.message.SystemMessage getLastSystemMessage(String threadId) {
        List<ChatMessage> messages = getMessages(threadId);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg instanceof dev.langchain4j.data.message.SystemMessage) {
                return (dev.langchain4j.data.message.SystemMessage) msg;
            }
        }
        return null;
    }

    /**
     * 获取消息数量
     */
    public int getMessageCount(String threadId) {
        List<ChatMessage> messages = getMessages(threadId);
        return messages == null ? 0 : messages.size();
    }

    /**
     * 获取最后n条消息
     */
    public String  getStringLastHistory(String threadId,int lastMessageCount){
        List<ChatMessage> messages = getMessages(threadId);
        if(messages == null || messages.isEmpty()){
            messages=List.of(UserMessage.from("我并没有问问题"));
        }
        if(messages.size() > lastMessageCount){
            messages=messages.subList(messages.size() - lastMessageCount, messages.size());
        }
        List<ChatMessage> chatMessage = getChatMessage(messages);
        return ChatMessageSerializer.messagesToJson(chatMessage);
    }


    public List<ChatMessage> getChatMessageLastHistory(String threadId, int lastMessageCount){
        List<ChatMessage> messages = getMessages(threadId);
        if(messages == null || messages.isEmpty()){
            messages=List.of(UserMessage.from("我并没有问问题"));
        }
        if(messages.size() > lastMessageCount){
            messages=messages.subList(messages.size() - lastMessageCount, messages.size());
        }
        return getChatMessage(messages);
    }

    public List<ChatMessage> getChatMessage(List<ChatMessage> messages){
        ArrayList<ChatMessage> chatMessages = new ArrayList<>();
        for(ChatMessage message:messages){
            if (message instanceof dev.langchain4j.data.message.AiMessage|| message instanceof dev.langchain4j.data.message.UserMessage) {
                chatMessages.add(message);
            }
        }
        return chatMessages;
    }
}