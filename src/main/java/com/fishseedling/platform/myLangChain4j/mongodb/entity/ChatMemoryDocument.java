package com.fishseedling.platform.myLangChain4j.mongodb.entity;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * MongoDB 会话记忆文档
 * 每个 memoryId 对应一条文档，存储完整的对话历史
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_memories")  // 指定集合名称
public class ChatMemoryDocument {

    /**
     * MongoDB 自动生成的主键
     */
    @Id
    private String id;

    /**
     * 会话唯一标识
     * - 普通用户：可使用 sessionId 或 前端生成的 UUID
     * - 管理员：可使用 userId 或 conversationId
     *
     * 添加唯一索引，确保每个会话只有一条文档
     */
    @Indexed(unique = true)  // 创建唯一索引，提升查询性能
    @Field("memory_id")
    private String memoryId;

    /**
     * 序列化后的消息列表 JSON 字符串
     * 使用 LangChain4j 的 ChatMessageSerializer.messagesToJson() 生成
     */
    @Field("messages")
    private String messages;

    /**
     * 创建时间（自动生成）
     */
    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    /**
     * 更新时间（自动更新）
     */
    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;

    /**
     * 可选：会话所属用户类型（USER/ADMIN）
     * 便于管理和查询
     */
    @Field("user_type")
    private String userType;

    /**
     * 可选：会话标题（取自第一条用户消息）
     */
    @Field("title")
    private String title;

    /**
     * 可选：消息数量（冗余字段，便于快速统计）
     */
    @Field("message_count")
    private Integer messageCount;
}