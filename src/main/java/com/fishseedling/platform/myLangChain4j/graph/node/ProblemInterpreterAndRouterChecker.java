package com.fishseedling.platform.myLangChain4j.graph.node;


import cn.hutool.json.JSONUtil;
import com.fishseedling.platform.myLangChain4j.graph.common.MultipleAiServiceMemory;
import com.fishseedling.platform.myLangChain4j.graph.common.UserSessionState;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.KnowledgeChat;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.ProblemInterpreterChat;
import com.fishseedling.platform.myLangChain4j.graph.pojo.aiResult.ProblemInterpreterResult;
import com.fishseedling.platform.myLangChain4j.graph.utils.StreamingHelperUtil;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresChatMessageStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * 问题路由节点
 * <p>Agent 工作流入口，根据用户类型（普通用户/管理员）和对话历史，
 * 通过 LLM 判断应路由到哪个下游处理节点（Knowledge、ApiSearch、SqlRetrieval、SmallTalk等）
 *
 * @author 贺畅
 */
@Component
@Slf4j
public class ProblemInterpreterAndRouterChecker implements AsyncNodeAction<UserSessionState> {
    @Resource
    MultipleAiServiceMemory multipleAiServiceMemory;

    /**
     * 执行路由判断并返回下一节点
     *
     * @param userSessionState 当前图状态，包含用户消息和会话信息
     * @return CompletableFuture 包含 next（下一节点名称）和 _streaming_messages（流式生成器）
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(UserSessionState userSessionState) {

        StreamingChatGenerator<MessagesState<ChatMessage>> generator = StreamingHelperUtil.bridge("Problem", userSessionState, null);
        generator.handler().onPartialResponse("🔍 正在进行路由节点选择，请稍候...");

        String s = userSessionState.getUserType();
        s=(s == null || s.isEmpty()) ?"普通用户":"管理员";

        ProblemInterpreterChat problemChat = multipleAiServiceMemory.getProblemChat();

        String lastNMessagesList = multipleAiServiceMemory.getChatMemoryStore().getStringLastHistory(userSessionState.getThreadId(), 4);

        log.debug("===当前节点为Problem=====历史消息为========");
        log.debug(JSONUtil.toJsonStr(lastNMessagesList));
        ProblemInterpreterResult chatResult = problemChat.chat(s,lastNMessagesList);

        log.debug("===当前节点为Problem=====路由理由========");
        log.debug(chatResult.getReason());

        log.debug("===当前节点为Problem===路由完毕下一节点为:==========");
        log.debug(chatResult.getNext());


        if(chatResult.getNext().equals("FINISH")){
            StreamingHelperUtil.ResponseStream(generator,userSessionState,chatResult.getFinishMsg(),multipleAiServiceMemory.getChatMemoryStore());
        }else {
            StreamingHelperUtil.ResponseStreamNotStore(generator,"路由节点完毕");
        }
        return CompletableFuture.completedFuture(Map.of("next",chatResult.getNext(),"_streaming_messages", generator));
    }

}

