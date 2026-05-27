package com.fishseedling.platform.myLangChain4j.graph.node;


import cn.hutool.Hutool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fishseedling.platform.myLangChain4j.graph.common.MultipleAiServiceMemory;
import com.fishseedling.platform.myLangChain4j.graph.common.UserSessionState;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.SqlRetrievalChat;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.SqlRetrievalChatOrganize;
import com.fishseedling.platform.myLangChain4j.graph.utils.StreamingHelperUtil;
import com.fishseedling.platform.myLangChain4j.graph.utils.TableSchemasFromSqlUtil;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresChatMessageStore;
import dev.langchain4j.data.message.*;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * SQL 生成与执行节点
 * <p>通过 LLM 将用户自然语言转换为 SQL 并执行（仅管理员），
 * 支持 SELECT/INSERT/UPDATE/DELETE，查询结果自动格式化为表格
 *
 * @author 贺畅
 */
@Component
@Slf4j
public class SqlRetrievalChecker implements AsyncNodeAction<UserSessionState> {
    @Resource
    MultipleAiServiceMemory multipleAiServiceMemory;

    /**
     * 执行 Text-to-SQL：LLM 生成 SQL → JDBC 执行 → 结果格式化 → 流式返回
     *
     * @param userSessionState 当前图状态，包含用户消息和会话信息
     * @return CompletableFuture 包含 next="FINISH" 和 _streaming_messages
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(UserSessionState userSessionState) {
        StreamingChatGenerator<MessagesState<ChatMessage>> generator = StreamingHelperUtil.bridge(
                "SqlRetrieval",
                userSessionState,
                (s) -> {
                    multipleAiServiceMemory.getChatMemoryStore().addMessages(userSessionState.getThreadId(),s.aiMessage());
                    return Map.of();
                }
        );
        generator.handler().onPartialResponse("生成sql语句进行操作中....");
        List<ChatMessage> chatMessage = multipleAiServiceMemory.getChatMemoryStore().getChatMessageLastHistory(userSessionState.getThreadId(),6);
        String messagesToJson = ChatMessageSerializer.messagesToJson(chatMessage);
        SqlRetrievalChat sqlRetrievalChat = multipleAiServiceMemory.getSqlRetrievalChat("aa.sql");
        TokenStream tokenStreamParent;
        try {
            tokenStreamParent = sqlRetrievalChat.chat(messagesToJson, ((UserMessage) chatMessage.get(chatMessage.size() - 1)).singleText());
        }catch (Exception e){
            generator.handler().onPartialResponse("生成sql语句失败，请换个问法吧....");
            return CompletableFuture.completedFuture(Map.of("next", "FINISH", "_streaming_messages", generator));
        }
        generator.handler().onPartialResponse("生成sql语句成功。sql语句为:");
        tokenStreamParent.onPartialResponse(generator.handler()::onPartialResponse)
                .onCompleteResponse(response -> {
                            DataSource mysql = multipleAiServiceMemory.getDynamicRoutingDataSource().getDataSource("mysql");
                            String chatResponseMsg = "";
                            Statement statement=null;
                            boolean isQuery=false;
                            try {
                                Connection connection = mysql.getConnection();
                                statement = connection.createStatement();
                                isQuery = statement.execute(response.aiMessage().text());
                            } catch (SQLException e) {
                                chatResponseMsg="执行sql失败...";
                                generator.handler().onPartialResponse(chatResponseMsg);
                                generator.handler().onCompleteResponse(ChatResponse.builder()
                                        .aiMessage(AiMessage.from(response.aiMessage().text()+chatResponseMsg)).build());
                                return;
                            }
                            chatResponseMsg ="执行sql成功!";
                            if (isQuery) {
                                generator.handler().onPartialResponse(chatResponseMsg + "\n" + "正在获取结果集....");
                                // 这是查询语句（SELECT），获取结果集
                                ResultSet rs = null;
                                List<Map<String, Object>> results = null;
                                try {
                                    rs = statement.getResultSet();
                                    results= extractResultSet(rs);
                                } catch (SQLException e) {
                                    generator.handler().onPartialResponse("获取结果集失败....");
                                    generator.handler().onCompleteResponse(
                                            ChatResponse.builder()
                                                    .aiMessage(AiMessage.from(response.aiMessage().text()+chatResponseMsg + "\n" + "获取结果集失败....")).build()
                                            );
                                    return;
                                }
                                generator.handler().onPartialResponse("获取结果集成功...");
                                log.debug("======正在美化sql输出结果:==sql结果为========");
                                log.debug(String.valueOf(rs));
                                SqlRetrievalChatOrganize sqlRetrievalChatOrganize = multipleAiServiceMemory.getSqlRetrievalChatOrganize();
                                String tableSchemas ="";
                                if(StrUtil.isNotEmpty(multipleAiServiceMemory.getTableSchemasMap().get("aa.sql"))){
                                    log.debug("=========aa.sql在multipleAiServiceMemory中未初始化================");
                                    tableSchemas=multipleAiServiceMemory.getTableSchemasMap().get("aa.sql");
                                }else  {
                                    tableSchemas = TableSchemasFromSqlUtil.extractTableSchemasFromSql("aa.sql");
                                }
                                TokenStream tokenStream = sqlRetrievalChatOrganize.chatOrganize(tableSchemas, JSONUtil.toJsonPrettyStr(results));
                                generator.handler().onPartialResponse("正在美化结果集...");
                                tokenStream.onPartialResponse(partialSon -> {
                                    generator.handler().onPartialResponse(partialSon);
                                }).onCompleteResponse(responseSon -> {
                                    log.debug("======美化sql输出结果完成:==========");
                                    generator.handler().onPartialResponse("美化结果集完成...");
                                    generator.handler().onCompleteResponse(
                                            ChatResponse.builder()//这个只是给mapResult中参数传递作用
                                                    .aiMessage(AiMessage.from(response.aiMessage().text()+"\n执行sql成功\n"+responseSon.aiMessage().text())).build()
                                    );
                                }).onError(error ->
                                        generator.handler().onError(error)
                                ).start();
                                return;
                            }
                            generator.handler().onPartialResponse(chatResponseMsg);
                            generator.handler().onCompleteResponse(ChatResponse.builder()//这个只是给mapResult中参数传递作用
                                    .aiMessage(AiMessage.from(response.aiMessage().text()+chatResponseMsg))
                                    .build());//触发generator 中mapResult(aiMessage -> ...) 逻辑。

                        }
                )
                .onError(error ->
                        generator.handler().onError(error)
                )
                .start(); // 启动流式传输
        log.debug("======调用SqlRetrieval点完毕内容为:==========");
        return CompletableFuture.completedFuture(Map.of("next", "FINISH", "_streaming_messages", generator));
    }


    public List<Map<String, Object>> extractResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        if (rs == null) {
            return results;
        }

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                // 处理 null 值，可以转换为空字符串或保留 null
                row.put(columnName, value != null ? value : "");
            }
            results.add(row);
        }

        return results;
    }


}
