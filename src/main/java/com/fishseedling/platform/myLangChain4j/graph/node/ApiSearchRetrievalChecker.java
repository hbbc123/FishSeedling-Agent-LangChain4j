package com.fishseedling.platform.myLangChain4j.graph.node;

import cn.hutool.json.JSONUtil;
import com.fishseedling.platform.myLangChain4j.graph.common.MultipleAiServiceMemory;
import com.fishseedling.platform.myLangChain4j.graph.common.UserSessionState;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.ApiSearchChat;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.QueryJsonOrganizeChat;
import com.fishseedling.platform.myLangChain4j.graph.utils.StreamingHelperUtil;
import com.fishseedling.platform.myLangChain4j.graph.utils.UniversalApiInvokerUtil;
import com.fishseedling.platform.myLangChain4j.milvus.entity.ApiDefinition;
import com.fishseedling.platform.myLangChain4j.milvus.service.MilvusSyncHandBookService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * API 检索与执行节点
 * <p>通过 Milvus 向量库匹配合适的 API，调用 HTTP 接口获取数据，
 * 然后将结果通过 QueryJsonOrganizeChat 格式化为用户可读内容
 *
 * @author 贺畅
 */
@Component
@Data
@Slf4j
public class ApiSearchRetrievalChecker implements AsyncNodeAction<UserSessionState> {
    @Resource
    MultipleAiServiceMemory multipleAiServiceMemory;
    @Resource
    MilvusSyncHandBookService milvusSyncService;

    /**
     * 执行 API 检索：向量匹配 → HTTP 调用 → LLM 格式化 → 流式返回
     *
     * @param userSessionState 当前图状态
     * @return CompletableFuture 包含 next="FINISH" 和 _streaming_messages
     */
    public CompletableFuture<Map<String, Object>> apply(UserSessionState userSessionState) {
        ChatMessage lastUserMessage = userSessionState.messages().get(userSessionState.messages().size() - 1);
        String text = ((dev.langchain4j.data.message.UserMessage) lastUserMessage).singleText();


        StreamingChatGenerator<MessagesState<ChatMessage>> generator = StreamingHelperUtil.bridge(
                "ApiSearch", userSessionState,
                (resp) -> {
                    log.debug("保存API消息: {}", resp.aiMessage().text());
                    multipleAiServiceMemory.getChatMemoryStore()
                            .addMessages(userSessionState.getThreadId(), resp.aiMessage());
                    return Map.of();
                }
        );
        generator.handler().onPartialResponse("🔍 正在检索api库，请稍候...\n\n");

        List<ApiDefinition> apiPath = milvusSyncService.findApiPath(text, 1);
        ApiSearchChat apiSearchChat = multipleAiServiceMemory.getApiSearchChat();

        //未匹配到 API：尝试用历史记录整理用户意图后重试
        if (apiPath == null || apiPath.isEmpty()) {
            generator.handler().onPartialResponse("🔍没有找到对应api，正在加载历史记录重新检索...\n\n");
            String lastMessagesString = multipleAiServiceMemory.getChatMemoryStore()
                    .getStringLastHistory(userSessionState.getThreadId(), 4);
            List<ChatMessage> lastNMessagesList = multipleAiServiceMemory.getChatMemoryStore()
                    .deserializeMessages(lastMessagesString);

            if (lastNMessagesList.isEmpty() || lastNMessagesList.size() <= 1) {
                // 无对话历史，无法推断意图
                StreamingHelperUtil.ResponseStreamNotStore(generator,"查询失败，助手无法理解您的意思，请换个问法吧。");
                return CompletableFuture.completedFuture(Map.of("next", "FINISH", "_streaming_messages", generator));
            }
            // 用 LLM 整理历史对话，提取用户意图后重新检索
            ChatMessage lastMessage = lastNMessagesList.remove(lastNMessagesList.size() - 1);
            AiMessage aiMessage = apiSearchChat.organizeHistory(
                    JSONUtil.toJsonStr(lastNMessagesList), JSONUtil.toJsonStr(lastMessage));
            apiPath = milvusSyncService.findApiPath(aiMessage.text(), 1);
        }


        if (apiPath == null || apiPath.isEmpty()) {
            StreamingHelperUtil.ResponseStreamNotStore(generator,"查询失败，助手加载对话记录也无法理解您的意思，请换个问法吧。");
            return CompletableFuture.completedFuture(Map.of("next", "FINISH", "_streaming_messages", generator));
        }


        ApiDefinition apiDefinition = apiPath.get(0);
        log.debug("======从向量库中获取对应的ApiDefinition元数据: {}", apiDefinition);
        Map<String, Object> chatResult = apiSearchChat.chat(
                apiDefinition.getMethod(), apiDefinition.getPath(),
                apiDefinition.getParametersDefinition(), text);
        log.debug("======api参数为: {}", chatResult);
        String resultJson ="";
        try {
            resultJson = UniversalApiInvokerUtil.execute(chatResult, String.valueOf(chatResult.get("path")));
            log.debug("======http调用api节点完毕内容为: {}", resultJson);

        } catch (Exception e) {
            log.error("api调用失败", e);
            StreamingHelperUtil.ResponseStreamNotStore(generator, "api调用失败，请重新换个问法吧！");
            return CompletableFuture.completedFuture(Map.of("next", "FINISH", "_streaming_messages", generator));
        }
        // 通过 LLM 格式化结果为用户可读内容
        QueryJsonOrganizeChat queryJsonChat = multipleAiServiceMemory.getQueryJsonChat();
        StreamingHelperUtil.TokenStreamBuild(generator,
                queryJsonChat.chat(resultJson, apiDefinition.getPath(), "11"));
        return CompletableFuture.completedFuture(Map.of("next", "FINISH", "_streaming_messages", generator));
    }

}
