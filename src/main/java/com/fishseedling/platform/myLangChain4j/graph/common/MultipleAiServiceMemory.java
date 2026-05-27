package com.fishseedling.platform.myLangChain4j.graph.common;

import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.*;
import com.fishseedling.platform.myLangChain4j.graph.utils.TableSchemasFromSqlUtil;
import com.fishseedling.platform.myLangChain4j.llmTodo.IqsTool;
import com.fishseedling.platform.myLangChain4j.model.WebSearchEngineConfig;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresAutomaticChatMessageStore;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.web.search.WebSearchEngine;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.HashMap;

/**
 * AI 服务工厂类
 * <p>统一管理和创建各类 LangChain4j AiService 实例，包括闲聊、SQL生成、路由判断等功能。
 * 核心职责：封装 AiService 的创建配置（模型、记忆、检索器、工具等），对外提供简洁的获取方法。
 *
 * @author 贺畅
 * @since 2025
 */
@Component
@Data
public class MultipleAiServiceMemory {

    @Value("${save-records.user-records:10}")
    private Integer userRecords;
    @Value("${save-records.admin-record:20}")
    private Integer adminRecord;
    @Value("${websearch.max-results:20}")
    private Integer maxResults;

    @Resource
    private PostgresAutomaticChatMessageStore chatMemoryStore;
    @Resource
    private ChatModel MyOpenAiChatModel;
    @Resource
    private WebSearchEngine MyWebSearchEngine;

    @Resource
    private DynamicRoutingDataSource dynamicRoutingDataSource;


    @Resource
    OpenAiStreamingChatModel MyOpenAiStreamingChatModel;

    @Resource
    IqsTool iqsTool;

    HashMap<String,String >  tableSchemasMap=new HashMap<>();



    // 核心方法：根据用户类型获取对应的 maxMessages
    private int getMaxMessages(String userType) {
        return userType!=null&&(!userType.isEmpty()) ? adminRecord : userRecords;
    }

    // 核心方法：创建 ChatMemoryProvider
    private ChatMemoryProvider createChatMemoryProvider(int maxMessages) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }


    private ContentRetriever createIQSRetrievalProvider() {
        return WebSearchContentRetriever.builder()
                .webSearchEngine(MyWebSearchEngine)   // 搜索引擎
                .maxResults(maxResults)                       // 最多返回2条搜索结果
                .build();
    }

    private ContentRetriever createSqlDatabaseContentRetriever(String sqlPath){
        DataSource mysqlDataSource = dynamicRoutingDataSource.getDataSource("mysql");
        String tableSchemas =null;
        if(StrUtil.isEmpty(tableSchemasMap.get(sqlPath))){
            tableSchemas = TableSchemasFromSqlUtil.extractTableSchemasFromSql(sqlPath);
            tableSchemasMap.put(sqlPath, tableSchemas);
        }else {
            tableSchemas = tableSchemasMap.get(sqlPath);
        }
        return SqlDatabaseContentRetriever.builder()
                .dataSource(mysqlDataSource)
                .chatModel(MyOpenAiChatModel)
                .sqlDialect("MySQL")
                .databaseStructure(tableSchemas)
                .build();
    }

    // 核心方法：创建任意类型的 AiServices


    private <T> T createAiService(Class<T> serviceClass, int maxMessages) {
        return AiServices.builder(serviceClass)
                .streamingChatModel(MyOpenAiStreamingChatModel)
                .chatModel(MyOpenAiChatModel)
                .chatMemoryProvider(createChatMemoryProvider(maxMessages))
                .build();
    }
    // 核心方法：创建任意类型的 AiServices
    private <T> T createAiService(Class<T> serviceClass,ContentRetriever contentRetriever) {
        return AiServices.builder(serviceClass)
                .chatModel(MyOpenAiChatModel)
                .streamingChatModel(MyOpenAiStreamingChatModel)
                .contentRetriever(contentRetriever)
                .build();
    }

    // 核心方法：创建任意类型的 AiServices
    private <T,X> T createAiService(Class<T> serviceClass,ContentRetriever contentRetriever,X toolClass) {
        return AiServices.builder(serviceClass)
                .chatModel(MyOpenAiChatModel)
                .streamingChatModel(MyOpenAiStreamingChatModel)
                .contentRetriever(contentRetriever)
                .tools(toolClass)
                .build();
    }

    // 核心方法：创建任意类型的 AiServices
    private <T> T createAiService(Class<T> serviceClass) {
        return AiServices.builder(serviceClass)
                .chatModel(MyOpenAiChatModel)
                .streamingChatModel(MyOpenAiStreamingChatModel)
                .build();
    }


    // ==================== 对外提供的方法 ====================

    /**
     * 创建闲聊对话助手
     *
     * @param userType 用户类型（管理员/null=普通用户），影响记忆窗口大小
     * @return SmallTalkChat 带记忆和人格设定的流式闲聊接口
     */
    public SmallTalkChat getSmallTalkChat(String userType) {
        return createAiService(SmallTalkChat.class, getMaxMessages(userType));
    }

    /**
     * 创建 API 参数提取助手
     *
     * @return ApiSearchChat 根据用户输入提取 API 调用参数的接口
     */
    public ApiSearchChat getApiSearchChat() {
        return createAiService(ApiSearchChat.class);
    }

    /**
     * 创建互联网搜索助手
     *
     * @return IQSRetrievalchat 带 WebSearch 检索器和 IQS 工具的流式接口
     */
    public IQSRetrievalchat getIQSRetrievalChat() {
        return createAiService(IQSRetrievalchat.class,createIQSRetrievalProvider(),iqsTool);
    }

    /**
     * 创建知识库问答助手
     *
     * @return KnowledgeChat 用于养殖知识库检索的流式接口
     */
    public KnowledgeChat getKnowledgeChat(){
        return createAiService(KnowledgeChat.class);
    }

    
    /**
    * 方法描述
    * @param path 文件名
    * @return SqlRetrievalChat      用于根据用户内容生产sql语句的流式接口
    */
    public SqlRetrievalChat getSqlRetrievalChat(String path) {
        ContentRetriever sqlDatabaseContentRetriever = createSqlDatabaseContentRetriever(path);
        return AiServices.builder(SqlRetrievalChat.class)
                .chatModel(MyOpenAiChatModel)
                .streamingChatModel(MyOpenAiStreamingChatModel)
                .contentRetriever(sqlDatabaseContentRetriever)
                .build();
    }

    /**
     * 创建 SQL的结果格式化助手
     *
     * @return SqlRetrievalChatOrganize 将 SQL 查询结果转为 Markdown 表格的流式接口
     */
    public SqlRetrievalChatOrganize getSqlRetrievalChatOrganize() {
        return createAiService(SqlRetrievalChatOrganize.class);
    }

    /**
     * 创建问题解释与路由判断助手
     *
     * @return ProblemInterpreterChat 分析用户意图并返回路由决策的接口
     */
    public ProblemInterpreterChat getProblemChat(){
        return createAiService(ProblemInterpreterChat.class);
    }

    /**
     * 创建 JSON 数据格式化助手
     *
     * @return QueryJsonOrganizeChat 将 JSON 数据格式化为 HTML 卡片的流式接口
     */
    public QueryJsonOrganizeChat getQueryJsonChat(){
        return createAiService(QueryJsonOrganizeChat.class);
    }
}