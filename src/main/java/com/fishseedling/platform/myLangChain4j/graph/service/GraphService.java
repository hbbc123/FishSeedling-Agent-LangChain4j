package com.fishseedling.platform.myLangChain4j.graph.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.fishseedling.platform.myLangChain4j.graph.common.MultipleAiServiceMemory;
import com.fishseedling.platform.myLangChain4j.graph.common.UserSessionState;
import com.fishseedling.platform.myLangChain4j.graph.node.*;
import com.fishseedling.platform.myLangChain4j.graph.pojo.dto.AiReception;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresCheckpointStore;
import com.fishseedling.platform.util.JwtUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.langchain4j.serializer.std.ChatMesssageSerializer;
import org.bsc.langgraph4j.langchain4j.serializer.std.ToolExecutionRequestSerializer;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Service
@DS("postgresql")
@Slf4j
public class GraphService {

    @Resource
    ProblemInterpreterAndRouterChecker problemInterpreterAndRouterChecker;
    @Resource
    ApiSearchRetrievalChecker apiSearchRetrievalChecker;
    @Resource
    SqlRetrievalChecker sqlRetrievalChecker;

    @Resource
    private PostgresCheckpointStore postgresqlDataSource;

    @Resource
    SmallTalkChecker smallTalkChecker;
    @Resource
    KnowledgeBaseChecker  knowledgeBaseChecker;

    @Resource
    IQSRetrievalChecker  iqsRetrievalChecker;

    @Resource
    MultipleAiServiceMemory multipleAiServiceMemory;

    @Resource
    JwtUtil jwtUtil;

    public void receiveAndProcess(AiReception aiReception, HttpServletRequest request){
        log.debug("====用户传递参数为======");
        log.debug(String.valueOf(aiReception));
        // 4. 编译配置：启用检查点，并设置 releaseThread=true
        // releaseThread=true 表示图执行完成后释放线程（清理检查点）
        multipleAiServiceMemory.getChatMemoryStore().addMessages(aiReception.getThreadId(),UserMessage.from(aiReception.getMsg()));
        var compileConfig = CompileConfig.builder()
                .checkpointSaver(postgresqlDataSource)    // 使用 MySQL 检查点保存器
                .releaseThread(false)        // ⭐ 释放线程（清理历史状态）
                .build();
        StateGraph<UserSessionState> userSessionStateStateGraph = flowchartGeneration();
        CompiledGraph<UserSessionState> compile=null;
        try {
            compile = userSessionStateStateGraph.compile(compileConfig);
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }

        String msg = aiReception.getMsg();
        UserMessage userMessage = UserMessage.from(msg);
        // 从token获取ID
        String token = request.getHeader("Authorization");
        Integer userId;
        if (token != null && token.startsWith("Bearer")) {
            token = token.substring(7);
            userId = jwtUtil.getAdminIdFromToken(token);
        } else {
            userId = null;
        }

        var runnableConfig = RunnableConfig.builder().threadId(aiReception.getThreadId()) .build();

        Map<String, Object> messages = new HashMap<>();
        messages.put("messages", UserMessage.from(aiReception.getMsg()));
        messages.put("userType", userId);  // 允许 null
        messages.put("threadId", aiReception.getThreadId());  // 允许 null

        log.debug("\n=== 流式输开始 ===");

        for (var output : compile.stream(messages,runnableConfig)) {
            if (output instanceof StreamingOutput streaming) {
                System.out.print(streaming.chunk());
            }
        }

        log.debug("\n=== 流式输出完成 ===");

//        Optional<UserSessionState> invoke;
//        try {
//            invoke = compile.invoke(messages, runnableConfig);
//        } catch (Exception e) {
//            throw new RuntimeException("Agent执行失败: " + e.getMessage(), e);
//        }
//
//
//        if (invoke.isEmpty()) {
//            throw new RuntimeException("Agent执行失败，未返回结果");
//        }
//
//
//        try {
//            postgresSaver.release(runnableConfig);
//        } catch (Exception e) {
//            // 记录日志但不抛出，避免覆盖主异常
//            System.err.println("释放 CheckpointSaver 失败: " + e.getMessage());
//        }
    }

    // ========== 新增：带 SseEmitter 的重载方法 ==========
    public void receiveAndProcess(AiReception aiReception, HttpServletRequest request, SseEmitter emitter) {
        log.debug("====用户传递参数为======");
        log.debug(String.valueOf(aiReception));

        multipleAiServiceMemory.getChatMemoryStore()
                .addMessages(aiReception.getThreadId(), UserMessage.from(aiReception.getMsg()));

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(postgresqlDataSource)
                .releaseThread(false)
                .build();

        StateGraph<UserSessionState> userSessionStateStateGraph = flowchartGeneration();
        CompiledGraph<UserSessionState> compile;
        try {
            compile = userSessionStateStateGraph.compile(compileConfig);
        } catch (GraphStateException e) {
            emitter.completeWithError(e);
            return;
        }

        String token = request.getHeader("Authorization");
        Integer userId;
        if (token != null && token.startsWith("Bearer")) {
            token = token.substring(7);
            userId = jwtUtil.getAdminIdFromToken(token);
        } else {
            userId = null;
        }

        var runnableConfig = RunnableConfig.builder()
                .threadId(aiReception.getThreadId())
                .build();

        Map<String, Object> messages = new HashMap<>();
        messages.put("messages", UserMessage.from(aiReception.getMsg()));
        messages.put("userType", userId);
        messages.put("threadId", aiReception.getThreadId());

        // ========== 异步执行图，避免阻塞 Tomcat 线程 ==========
        CompletableFuture.runAsync(() -> {
            try {
                log.debug("\n=== 流式输出开始 ===");

                ObjectMapper om = new ObjectMapper();
                for (var output : compile.stream(messages, runnableConfig)) {
                    if (output instanceof StreamingOutput streaming) {
                        // JSON 序列化自动转义 \n \" \\ 等特殊字符
                        emitter.send(SseEmitter.event().name("message")
                                .data(om.writeValueAsString(streaming.chunk())));
                    }
                }

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("[DONE]"));
                emitter.complete();
                log.debug("\n=== 流式输出完成 ===");

            } catch (Exception e) {
                System.err.println("图执行失败: " + e.getMessage());
                emitter.completeWithError(e);
            } finally {
                try {
                    postgresqlDataSource.release(runnableConfig);
                    log.debug("CheckPoint 已释放: " + runnableConfig.threadId());
                } catch (Exception ex) {
                    System.err.println("释放 CheckPoint 失败: " + ex.getMessage());
                }
            }
        });

        // 注册超时和错误回调（清理资源）
        emitter.onTimeout(() -> System.err.println("SSE 连接超时: " + aiReception.getThreadId()));
        emitter.onError(e -> System.err.println("SSE 连接错误: " + e.getMessage()));
    }

    public StateGraph<UserSessionState> flowchartGeneration () {
      StateGraph<UserSessionState> userSessionStateStateGraph=null;
      try {
          userSessionStateStateGraph = new StateGraph<UserSessionState>(UserSessionState.SCHEMA, serializer())

                  // ============ 1. 添加业务节点 ============
                  .addNode("Problem", problemInterpreterAndRouterChecker)
                  .addNode("Knowledge", knowledgeBaseChecker)
                  .addNode("ApiSearch", apiSearchRetrievalChecker)
                  .addNode("IQSRetrieval", iqsRetrievalChecker)
                  .addNode("SqlRetrieval", sqlRetrievalChecker)
                  .addNode("SmallTalk", smallTalkChecker)

                  // ============ 2. 添加边 ============
                  .addEdge(START, "Problem")
                  // ============ 3. Problem 条件路由 ============
                  .addConditionalEdges("Problem",
                          edge_async(UserSessionState::next),
                          Map.of(
                                  "Knowledge", "Knowledge",
                                  "ApiSearch", "ApiSearch",
                                  "IQSRetrieval", "IQSRetrieval",
                                  "SqlRetrieval", "SqlRetrieval",
                                  "SmallTalk", "SmallTalk",
                                  "FINISH", END
                          )
                  )

                  // ============ 4. Knowledge 节点后的路由 ============
                  .addConditionalEdges("Knowledge",
                          edge_async(state -> {
                              String next = state.next();
                              if ("IQSRetrieval".equals(next)) {
                                  return "IQSRetrieval";
                              }
                              return END;
                          }),
                          Map.of(
                                  "IQSRetrieval", "IQSRetrieval",
                                  END, END
                          )
                  )

                  // ============ 5. 其他节点直接结束 ============
                  .addEdge("ApiSearch", END)
                  .addEdge("SmallTalk", END)
                  .addEdge("IQSRetrieval", END)
                  .addEdge("SqlRetrieval", END);

      } catch (GraphStateException e) {
          throw new RuntimeException(e);
      }
      return userSessionStateStateGraph;
  }



  public ObjectStreamStateSerializer<UserSessionState> serializer(){
      // 创建一个 ObjectStreamStateSerializer 实例，用于序列化 MessageState 类型
      // 参数 MessageState::new 是一个构造器引用，用于在反序列化时创建新的 MessageState 实例
      var stateSerializer = new ObjectStreamStateSerializer<UserSessionState>( UserSessionState::new );
      // 获取序列化器的映射器 (mapper) 来注册自定义序列化器
      stateSerializer.mapper()
              // 为 ToolExecutionRequest 类注册自定义序列化器，以便正确处理该类型的对象
              .register(ToolExecutionRequest.class, new ToolExecutionRequestSerializer() )
              // 为 ChatMessage 接口注册自定义序列化器，以便正确处理所有 ChatMessage 的实现类
              .register(ChatMessage.class, new ChatMesssageSerializer() );

      return stateSerializer;
  }

    public List<ChatMessage> getUserHistory(AiReception aiReception) {
        return multipleAiServiceMemory.getChatMemoryStore().getMessages(aiReception.getThreadId());
    }

    public void delUserHistory(AiReception aiReception){
      multipleAiServiceMemory.getChatMemoryStore().deleteMessages(aiReception.getThreadId());
    }
}
