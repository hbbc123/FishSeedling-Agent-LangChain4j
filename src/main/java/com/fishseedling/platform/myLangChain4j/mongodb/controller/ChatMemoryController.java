package com.fishseedling.platform.myLangChain4j.mongodb.controller;


import com.fishseedling.platform.myLangChain4j.mongodb.service.ChatMemoryService;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

/**
 * MongoDB 聊天记忆存储实现
 * 适配 LangChain4j 的 ChatMemoryStore 接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemoryController implements ChatMemoryStore {

    private final ChatMemoryService chatMemoryService;

    /**
     * 获取指定会话的所有消息
     * LangChain4j 框架会在每次对话开始时调用此方法
     */
    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        String memoryIdStr = memoryId.toString();
        String messagesJson = chatMemoryService.getMessagesJson(memoryIdStr);

        if (messagesJson == null) {
            return List.of();  // 返回空列表，表示新会话
        }

        try {
            return messagesFromJson(messagesJson);
        } catch (Exception e) {
            log.error("反序列化消息失败: memoryId={}", memoryIdStr, e);
            return List.of();
        }
    }

    /**
     * 更新指定会话的消息列表（全量替换）
     * LangChain4j 框架会在每轮对话后调用此方法
     */
    @Override
    public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        String memoryIdStr = memoryId.toString();
        String messagesJson = messagesToJson(messages);

        // 提取第一条用户消息作为标题（可选）
        String firstUserMessage = null;
        for (dev.langchain4j.data.message.ChatMessage msg : messages) {
            if (msg instanceof dev.langchain4j.data.message.UserMessage) {
                firstUserMessage = ((dev.langchain4j.data.message.UserMessage) msg).singleText();
                break;
            }
        }

        // 这里需要知道用户类型，可以从 ThreadLocal 或请求上下文获取
        String userType = getUserTypeFromContext();  // 需自行实现

        chatMemoryService.saveOrUpdate(memoryIdStr, messagesJson, userType, firstUserMessage);
        log.debug("保存会话历史: memoryId={}, messageCount={}", memoryIdStr, messages.size());
    }

    /**
     * 删除指定会话
     * 调用 ChatMemory.clear() 时触发
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String memoryIdStr = memoryId.toString();
        chatMemoryService.deleteByMemoryId(memoryIdStr);
        log.debug("删除会话: memoryId={}", memoryIdStr);
    }

    /**
     * 从上下文获取当前用户类型
     * 可通过 RequestContextHolder 或 SecurityContext 实现 todo  要实现
     */
    private String getUserTypeFromContext() {
        // 简单实现：从请求属性或 SecurityContext 获取
        // 示例：return UserContextHolder.getCurrentUserType();
        return "USER";  // 默认普通用户
    }
}