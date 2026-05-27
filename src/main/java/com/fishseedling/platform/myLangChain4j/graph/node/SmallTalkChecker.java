package com.fishseedling.platform.myLangChain4j.graph.node;

import com.fishseedling.platform.myLangChain4j.graph.common.MultipleAiServiceMemory;
import com.fishseedling.platform.myLangChain4j.graph.common.UserSessionState;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.SmallTalkChat;
import com.fishseedling.platform.myLangChain4j.graph.utils.CustomChatMemoryUtil;
import com.fishseedling.platform.myLangChain4j.graph.utils.StreamingHelperUtil;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresChatMessageStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * 闲聊处理节点
 * <p>调用 SmallTalkChat 流式接口处理与鱼养殖行业无关的日常闲聊，
 * 自动管理对话记忆并保存到 chatMemoryStore
 *
 * @author 贺畅
 */
@Component
@Slf4j
public class SmallTalkChecker implements AsyncNodeAction<UserSessionState> {
    @Resource
    MultipleAiServiceMemory multipleAiServiceMemory;

    /**
     * 执行闲聊对话处理
     *
     * @param userSessionState 当前图状态，包含用户消息和会话信息
     * @return CompletableFuture 包含 next="FINISH" 和 _streaming_messages
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(UserSessionState userSessionState) {

        ChatMessage lastUserMessage = userSessionState.messages().get(userSessionState.messages().size() - 1);
        String text = ((dev.langchain4j.data.message.UserMessage) lastUserMessage).singleText();

        // 1. 获取带自动记忆的流式 AiService（记忆由框架托管，无需手动保存）
        SmallTalkChat smallTalkChat = multipleAiServiceMemory.getSmallTalkChat(userSessionState.getUserType());
        StreamingChatGenerator<MessagesState<ChatMessage>> generator = StreamingHelperUtil.bridge(
                "SmallTalk",
                userSessionState,
                null
        );
        generator.handler().onPartialResponse("🔍正在调用聊天节点，请稍候...\n\n");
        ChatMessage chatMessage = multipleAiServiceMemory.getChatMemoryStore().deleteLastMessages(userSessionState.getThreadId());
        StreamingHelperUtil.TokenStreamBuild(
                generator,
                smallTalkChat.chat(userSessionState.getThreadId(), ((UserMessage) chatMessage).singleText())
        );


        log.debug("======调用SmallTalk节点完毕内容为:==========");
        return CompletableFuture.completedFuture(Map.of("next", "FINISH","_streaming_messages", generator));
    }

//    @Override
//    public CompletableFuture<Map<String, Object>> apply(UserSessionState userSessionState) {
//
//        ChatMessage lastUserMessage = userSessionState.messages().get(userSessionState.messages().size() - 1);
//        String text = ((dev.langchain4j.data.message.UserMessage) lastUserMessage).singleText();
//        SmallTalkChat smallTalkChat = multipleAiServiceMemory.getSmallTalkChat(userSessionState.getUserType());
//        AiMessage chatResult = smallTalkChat.chat(userSessionState.getThreadId(), text);
//
//        log.debug("======调用SmallTalk节点完毕内容为:==========");
//        log.debug(chatResult.text());
//        return CompletableFuture.completedFuture(Map.of("next", "FINISH"));
//    }
}
