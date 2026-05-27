package com.fishseedling.platform.myLangChain4j.mongodb.service;


import com.fishseedling.platform.myLangChain4j.mongodb.entity.ChatMemoryDocument;
import com.fishseedling.platform.myLangChain4j.mongodb.mapper.ChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

/**
 * 会话记忆服务
 * 实现 ChatMemoryStore 接口的核心逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final ChatMemoryRepository chatMemoryRepository;

    /**
     * 获取会话消息（反序列化前的原始 JSON）
     *
     * @param memoryId 会话唯一标识
     * @return 消息列表的 JSON 字符串，不存在则返回 null
     */
    @Transactional(readOnly = true)
    public String getMessagesJson(String memoryId) {
        Optional<ChatMemoryDocument> optional = chatMemoryRepository.findByMemoryId(memoryId);
        if (optional.isPresent()) {
            log.debug("加载会话历史: memoryId={}, messageCount={}",
                    memoryId, optional.get().getMessageCount());
            return optional.get().getMessages();
        }
        log.debug("新建会话: memoryId={}", memoryId);
        return null;  // 不存在则返回 null，调用方会初始化为空列表
    }

    /**
     * 保存或更新会话（全量替换）
     *
     * @param memoryId     会话唯一标识
     * @param messagesJson 序列化后的消息列表 JSON
     * @param userType     用户类型（USER/ADMIN）
     * @param firstMessage 首条用户消息（用于生成标题）
     */
    @Transactional
    public void saveOrUpdate(String memoryId, String messagesJson, String userType, String firstMessage) {
        Optional<ChatMemoryDocument> optional = chatMemoryRepository.findByMemoryId(memoryId);

        if (optional.isPresent()) {
            // 更新现有文档
            ChatMemoryDocument doc = optional.get();
            doc.setMessages(messagesJson);
            // 可选：更新消息数量（可根据 JSON 计算，此处简化）
            doc.setUpdatedAt(new Date());
            chatMemoryRepository.save(doc);
            log.debug("更新会话: memoryId={}", memoryId);
        } else {
            // 创建新文档
            ChatMemoryDocument doc = new ChatMemoryDocument();
            doc.setMemoryId(memoryId);
            doc.setMessages(messagesJson);
            doc.setUserType(userType);
            if (firstMessage != null && firstMessage.length() > 30) {
                doc.setTitle(firstMessage.substring(0, 30) + "...");
            } else {
                doc.setTitle(firstMessage);
            }
            doc.setMessageCount(estimateMessageCount(messagesJson));
            // createdAt 和 updatedAt 由 @CreatedDate/@LastModifiedDate 自动填充
            chatMemoryRepository.save(doc);
            log.debug("创建新会话: memoryId={}, userType={}", memoryId, userType);
        }
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteByMemoryId(String memoryId) {
        chatMemoryRepository.deleteByMemoryId(memoryId);
        log.debug("删除会话: memoryId={}", memoryId);
    }

    /**
     * 检查会话是否存在
     */
    public boolean exists(String memoryId) {
        return chatMemoryRepository.existsByMemoryId(memoryId);
    }

    /**
     * 估算消息数量（简单实现）
     * 生产环境可解析 JSON 精确计算
     */
    private int estimateMessageCount(String messagesJson) {
        if (messagesJson == null || messagesJson.isEmpty()) {
            return 0;
        }
        // 简单估算：统计 "type" 字段出现次数
        int count = messagesJson.split("\"type\"").length - 1;
        return count / 2;  // 每条消息有两个 type（user + assistant）
    }
}