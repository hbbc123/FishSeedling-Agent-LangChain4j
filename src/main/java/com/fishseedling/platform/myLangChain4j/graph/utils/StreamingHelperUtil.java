package com.fishseedling.platform.myLangChain4j.graph.utils;

import com.fishseedling.platform.myLangChain4j.graph.common.UserSessionState;
import com.fishseedling.platform.myLangChain4j.graph.node.IQSRetrievalChecker;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresAutomaticChatMessageStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Map;
import java.util.function.Function;
/**
 * 流式输出工具类
 * <p>封装 LangGraph4j StreamingChatGenerator 与 LangChain4j TokenStream 的桥接逻辑，
 * 提供 generator 创建、流式转发、模拟打字机效果、记忆持久化等功能
 *
 * @author 贺畅
 * @since 2026
 */
@Slf4j
public class StreamingHelperUtil {

    /**
     * 创建 StreamingChatGenerator 桥接器
     * <p>用于图节点中创建流式生成器，将 LLM 输出连接到图引擎的流式管道
     *
     * @param startingNode     起始节点名称（用于日志和状态追踪）
     * @param userSessionState 当前图状态，包含用户消息和会话信息
     * @param mapResult        流结束时的回调，将 ChatResponse 映射为图状态更新 Map，null 时返回空 Map
     * @return StreamingChatGenerator 配置好的流式生成器
     */
    public static StreamingChatGenerator<MessagesState<ChatMessage>> bridge(
            String startingNode,
            UserSessionState userSessionState,
            Function<ChatResponse, Map<String, Object>> mapResult

    ){
        return StreamingChatGenerator.<MessagesState<ChatMessage>>builder()
                .startingNode(startingNode)
                .startingState(userSessionState)
                .mapResult(mapResult==null?(s)->Map.of():mapResult)// mapResult 仅将完整消息映射到图状态，供下游节点读取
                .build();
    }

    /**
     * 将 TokenStream 桥接到 StreamingChatGenerator
     * <p>注册 onPartialResponse/onCompleteResponse/onError 回调并调用 start() 启动流
     *
     * @param generator    已创建的流式生成器
     * @param tokenStream LangChain4j 的流式 TokenStream
     * @return 传入的 generator（已绑定流）
     */
    public static StreamingChatGenerator<MessagesState<ChatMessage>> TokenStreamBuild(StreamingChatGenerator<MessagesState<ChatMessage>> generator , TokenStream tokenStream){
        // 3. ⚠️ 关键：桥接 TokenStream 与 Generator Handler

        tokenStream
                .onPartialResponse(partial ->{
                            // 将每个 Token 实时转发给前端
                            generator.handler().onPartialResponse(partial);
                        }
                )
                .onCompleteResponse(response ->{
                            log.debug(" =========流结束时通知 Generator（此时 AiService 已自动保存记忆）======");
                            generator.handler().onCompleteResponse(response);//触发generator 中mapResult(aiMessage -> ...) 逻辑。
                        }
                )
                .onError(error ->
                        generator.handler().onError(error)
                )
                .start(); // 启动流式传输
        return generator;
    }

    /**
     * 一步创建并桥接：创建 generator + 绑定 TokenStream
     *
     * @param startingNode     起始节点名称
     * @param userSessionState 当前图状态
     * @param mapResult        流结束回调
     * @param tokenStream      要桥接的 TokenStream
     * @return 已绑定的流式生成器
     */
    public static StreamingChatGenerator<MessagesState<ChatMessage>> BridgeTokenStream(
            String startingNode,
            UserSessionState userSessionState,
            Function<ChatResponse, Map<String, Object>> mapResult,
            TokenStream tokenStream
            ){
        StreamingChatGenerator<MessagesState<ChatMessage>> bridge = bridge(startingNode, userSessionState, mapResult);
        TokenStreamBuild(bridge,tokenStream);
        return bridge;
    }

    /**
     * 将 TokenStream 绑定到已有的 generator
     *
     * @param bridge       已创建的流式生成器
     * @param tokenStream 要桥接的 TokenStream
     * @return 已绑定的流式生成器
     */
    public static StreamingChatGenerator<MessagesState<ChatMessage>> BridgeTokenStream(
            StreamingChatGenerator<MessagesState<ChatMessage>> bridge,
            TokenStream tokenStream
    ){

        TokenStreamBuild(bridge,tokenStream);
        return bridge;
    }

    /**
     * 流式输出文本并保存到记忆
     * <p>模拟打字机效果逐字输出，完成后保存到 chatMemoryStore 并调用 onCompleteResponse
     *
     * @param generator        流式生成器
     * @param userSessionState 当前图状态（用于获取 threadId）
     * @param msg              要输出的完整文本
     * @param chatMemoryStore  对话记忆存储
     */
    public static void  ResponseStream(
            StreamingChatGenerator<MessagesState<ChatMessage>>generator,
            UserSessionState userSessionState,
            String msg,
            PostgresAutomaticChatMessageStore chatMemoryStore
    ){
        // 3. 流式输出（模拟打字机效果）
        ImitateStream(generator,msg);

        // 4. 保存到记忆存储
        chatMemoryStore.addMessages(userSessionState.getThreadId(), AiMessage.from(msg));

        // 5. 完成响应
        generator.handler().onCompleteResponse(
                ChatResponse.builder()//这个只是给mapResult中参数传递作用
                        .aiMessage(AiMessage.from(msg))
                        .build()
        );
    }

    /**
     * 流式输出文本但不保存到记忆
     *
     * @param generator 流式生成器
     * @param msg       要输出的完整文本
     */
    public static void  ResponseStreamNotStore(
            StreamingChatGenerator<MessagesState<ChatMessage>>generator,
            String msg
    ){
        // 3. 流式输出（模拟打字机效果）
        ImitateStream(generator,msg);


        // 5. 完成响应
        generator.handler().onCompleteResponse(
                ChatResponse.builder()
                        .aiMessage(AiMessage.from(msg))
                        .build()
        );
    }

    /**
     * 模拟打字机效果逐字符输出
     * <p>将字符串逐字符通过 generator 发送，每字符间隔 5ms
     *
     * @param generator 流式生成器
     * @param msg       要逐字输出的文本
     */
    public static void ImitateStream(StreamingChatGenerator<MessagesState<ChatMessage>> generator ,String msg){
        for (char c : msg.toCharArray()) {
            generator.handler().onPartialResponse(String.valueOf(c));
            try {
                Thread.sleep(5); // 控制输出速度
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

}
