package com.fishseedling.platform.myLangChain4j.graph.node;

import cn.hutool.json.JSONUtil;
import com.fishseedling.platform.myLangChain4j.graph.common.MultipleAiServiceMemory;
import com.fishseedling.platform.myLangChain4j.graph.common.UserSessionState;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.IQSRetrievalchat;
import com.fishseedling.platform.myLangChain4j.graph.pojo.dto.AiSearchResult;
import com.fishseedling.platform.myLangChain4j.graph.utils.StreamingHelperUtil;
import com.fishseedling.platform.myLangChain4j.llmTodo.IqsTool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServiceTokenStream;
import dev.langchain4j.service.TokenStream;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 互联网搜索节点
 * <p>通过 IQS 工具进行互联网搜索，将搜索结果流式返回并保存到对话记忆
 *
 * @author 贺畅
 */
@Component()
public class IQSRetrievalChecker implements AsyncNodeAction<UserSessionState> {

    @Resource
    IqsTool iqsTool;

    /**
     * 执行互联网搜索并流式返回结果
     *
     * @param userSessionState 当前图状态，包含用户消息和会话信息
     * @return CompletableFuture 包含 next="FINISH" 和 _streaming_messages
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(UserSessionState userSessionState) {
        ChatMessage lastUserMessage = userSessionState.lastMessage().orElse(null);
        String text = ((dev.langchain4j.data.message.UserMessage) lastUserMessage).singleText();

        // 创建 generator
        StreamingChatGenerator<MessagesState<ChatMessage>> generator = StreamingHelperUtil.bridge(
                "IQSRetrieval",
                userSessionState,
                (mapResult)->{
                    iqsTool.getMultipleAiServiceMemory().getChatMemoryStore().addMessages(
                            userSessionState.getThreadId(),
                            mapResult.aiMessage()
                    );
                    return Map.of();
                }
        );
        // 立即发送开始提示
        generator.handler().onPartialResponse("🔍 正在检索相关信息，请稍候...\n\n");

        IQSRetrievalchat iqsRetrievalChat = iqsTool.getMultipleAiServiceMemory().getIQSRetrievalChat();
        TokenStream tokenStream = iqsRetrievalChat.chatTool(
                iqsTool.getMultipleAiServiceMemory().getChatMemoryStore().getStringLastHistory(userSessionState.getThreadId(), 4),
                "请根据历史对话记录整理出我要问的内容，然后调用iqs工具"
        );
        StreamingHelperUtil.BridgeTokenStream(generator, tokenStream);


        return CompletableFuture.completedFuture(Map.of(
                "next", "FINISH",
                "_streaming_messages", generator
        ));
    }



}