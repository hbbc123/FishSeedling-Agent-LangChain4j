package com.fishseedling.platform.myLangChain4j.graph.utils;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomChatMemoryUtil implements ChatMemory {
    private final List<ChatMessage> messages = new ArrayList<>();

    public CustomChatMemoryUtil(Map<String,Object> map){
        messages.addAll(map.get("messages")==null?new ArrayList<>():(List<ChatMessage>)map.get("messages"));
    }
    @Override
    public Object id() {
        return null;
    }

    @Override
    public void add(ChatMessage chatMessage) {

    }

    @Override
    public List<ChatMessage> messages() {
        return messages;
    }

    @Override
    public void clear() {

    }
}
