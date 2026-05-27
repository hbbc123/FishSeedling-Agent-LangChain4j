package com.fishseedling.platform.myLangChain4j.graph.node;


import com.fishseedling.platform.myLangChain4j.graph.common.MultipleAiServiceMemory;
import com.fishseedling.platform.myLangChain4j.graph.common.UserSessionState;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.KnowledgeChat;
import com.fishseedling.platform.myLangChain4j.graph.utils.StreamingHelperUtil;
import com.fishseedling.platform.myLangChain4j.milvus.service.MilvusSyncHandBookService;
import com.fishseedling.platform.myLangChain4j.milvus.service.MilvusSyncHelpComentService;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresChatMessageStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 知识库检索节点
 * <p>通过 Milvus 向量数据库检索养殖手册和帮助内容，
 * 如果两个知识库都无结果则降级到 IQS 互联网搜索
 *
 *
 *          todo 可以改造为查询路由加iqsTool的工具的agent
 *          //    创建查询路由器（核心组件）---
 *         // 建立一个映射，将每个内容检索器与其描述关联起来
 *         // 这个描述会被 LLM 路由器理解，以决定查询应该交给哪个检索器
 *         Map<ContentRetriever, String> retrieverToDescription = new HashMap<>();
 *         retrieverToDescription.put(biographyContentRetriever, "水产养殖及个人信息");
 *         retrieverToDescription.put(termsOfUseContentRetriever, "北京");
 *
 *         // 使用 LLM 查询路由器它会根据用户问题和这些描述，智，能地选择最合适的检索器（们）
 *         QueryRouter queryRouter = new LanguageModelQueryRouter(chatModel, retrieverToDescription);
 *
 *         RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
 *                 .queryRouter(queryRouter)// 设置查询路由器
 *                 .build();
 *
 *         return AiServices.builder(Assistant.class)
 *                 .chatModel(chatModel)
 *                 .retrievalAugmentor(retrievalAugmentor)// 使用配置了路由器的 RAG 增强器
 *                 .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
 *                 .build();
 * @author 贺畅
 */
@Component
@Slf4j
public class KnowledgeBaseChecker  implements AsyncNodeAction<UserSessionState> {
    @Resource
    MilvusSyncHandBookService milvusSyncHandBookService;

    @Resource
    MilvusSyncHelpComentService milvusSyncHelpComentService;

    @Resource
    MultipleAiServiceMemory multipleAiServiceMemory;

    /**
     * 执行知识库检索并返回结果
     *
     * @param userSessionState 当前图状态，包含用户消息
     * @return CompletableFuture 包含 next（FINISH 或 IQSRetrieval）和 _streaming_messages
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(UserSessionState userSessionState) {

        ChatMessage chatMessage = userSessionState.lastMessage().orElse(null);
        String text =((dev.langchain4j.data.message.UserMessage) chatMessage).singleText();

        StreamingChatGenerator<MessagesState<ChatMessage>> generator = StreamingHelperUtil.bridge(
                "Knowledge",
                userSessionState,
                (mapResult) -> {
                    if(!mapResult.aiMessage().text().equals("error")){
                        multipleAiServiceMemory.getChatMemoryStore().addMessages(userSessionState.getThreadId(), mapResult.aiMessage());
                    }
                    return Map.of();
                }
        );
        generator.handler().onPartialResponse("🔍 正在检索知识库，请稍候...\n\n");
        List<EmbeddingMatch<TextSegment>> datalist = milvusSyncHandBookService.findHandbooks(text, 1);
        log.debug("=======手册知识库查找的数据========");
        log.debug(datalist.toString());

        if (datalist.isEmpty()) {
            log.debug("=======使用帮助知识库查找的数据========");
            datalist = milvusSyncHelpComentService.findHandbooks(text, 1);
        }
        log.debug("=======手册帮助库查找的数据========");
        log.debug(datalist.toString());
        if(datalist.isEmpty()){
            log.debug("======两个知识库都没找到进行iqs查询========");
            generator.handler().onPartialResponse("知识库未查询到内容,将进行isq检索...\n\n");
            generator.handler().onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("error"))
                    .build());
            return CompletableFuture.completedFuture(Map.of("next", "IQSRetrieval","_streaming_messages",generator));
        }
        StringBuilder zsTxt= new StringBuilder();
        for (EmbeddingMatch<TextSegment> handbook : datalist) {
            zsTxt.append("\\n").append(handbook.embedded().text());
        }

        KnowledgeChat knowledgeChat = multipleAiServiceMemory.getKnowledgeChat();
        StreamingHelperUtil.BridgeTokenStream( generator, knowledgeChat.chat(zsTxt.toString()));
        log.debug("======调用SKnowledge点完毕:==========");
        return CompletableFuture.completedFuture(Map.of("next","FINISH","_streaming_messages",generator));
    }




}
