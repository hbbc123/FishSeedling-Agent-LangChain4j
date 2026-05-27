package com.fishseedling.platform.myLangChain4j.mongodb.config;


import com.fishseedling.platform.myLangChain4j.mongodb.controller.ChatMemoryController;
import com.fishseedling.platform.myLangChain4j.mongodb.service.ChatMemoryService;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class ChatMemoryConfig {

    private final ChatMemoryController mongoChatMemoryStore;

    /**
     * 普通用户的 ChatMemoryProvider
     * 保留最近 10 条消息，会话级别（Session 销毁时不清除 MongoDB，但可设置过期）
     */
    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public ChatMemoryProvider userChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)  // 保留最近 10 条消息
                .chatMemoryStore(mongoChatMemoryStore)
                .build();
    }

    /**
     * 管理员的 ChatMemoryProvider
     * 保留最近 50 条消息，持久化到 MongoDB
     */
    @Bean
    public Function<String, ChatMemory> adminChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)  // 管理员保留更多消息
                .chatMemoryStore(mongoChatMemoryStore)
                .build();
    }
}