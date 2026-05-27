# LangChain4j + LangGraph4j 开发总结

> 鱼苗供需信息发布平台 - AI 智能助手模块

---

## 一、项目架构概览

```
用户输入
    │
    ▼
┌──────────────────────────────────────────┐
│         ProblemInterpreter (路由器)        │
│   LLM 分析意图 → 选择下游节点               │
└────┬──────┬──────┬──────┬──────┬─────────┘
     │      │      │      │      │
     ▼      ▼      ▼      ▼      ▼
 Knowledge ApiSearch SqlRetrieval IQS SmallTalk
 (知识库)  (API检索) (SQL执行) (搜索) (闲聊)
     │      │      │      │      │
     └──────┴──────┴──────┴──────┘
                    │
                    ▼
               SSE 流式输出 → 前端
```


### 技术栈

| 组件 | 技术 |
|---|---|
| LLM 模型 | DeepSeek (deepseek-chat / deepseek-v4-flash) |
| 嵌入模型 | Ollama (qwen3-embedding:0.6b) |
| 向量数据库 | Milvus |
| 对话记忆 | PostgreSQL (PostgresAutomaticChatMessageStore) |
| 图状态存储 | PostgreSQL (PostgresCheckpointStore) |
| 搜索引擎 | 阿里云 IQS WebSearchEngine |
| 流式传输 | SSE (Server-Sent Events) |

---

## 二、LangChain4j 官方 API 使用心得

### 2.1 @AiService 接口声明

LangChain4j 通过接口 + 注解的方式定义 AI 服务，核心注解：

```java
public interface SmallTalkChat {
    @SystemMessage("角色设定...")
    TokenStream chat(@MemoryId String memoryId, @UserMessage String s);
}
```

**关键注解**：
- `@SystemMessage`：定义 LLM 系统提示词
- `@MemoryId`：标记记忆 ID 参数，控制对话历史的读写
- `@UserMessage`：标记用户输入参数
- `@V("变量名")`：注入变量到 SystemMessage 模板

### 2.2 AiServices 构建器

```java
AiServices.builder(SmallTalkChat.class)
    .chatModel(chatModel)                    // 非流式模型（用于内部工具调用）
    .streamingChatModel(streamingModel)       // 流式模型（用于用户可见输出）
    .chatMemoryProvider(memoryProvider)       // 记忆管理
    .contentRetriever(sqlRetriever)           // RAG 检索器
    .tools(iqsTool)                           // 工具调用
    .build();
```

### 2.3 TokenStream 流式调用

```java
TokenStream tokenStream = aiService.chat(userMessage);
tokenStream
    .onPartialResponse(partial -> sendToFrontend(partial))   // 逐 token 推送
    .onCompleteResponse(response -> handleComplete(response))  // 流结束
    .onError(error -> handleError(error))                      // 异常
    .start();  // ← 必须调用 start() 才会真正发起 LLM 请求
```

**注意事项**：
- 注册回调后必须调用 `.start()`，否则 LLM 请求不会发起
- `onPartialResponse` 逐字符推送，需要前端做 Markdown 实时渲染
- `onCompleteResponse` 中的 `ChatResponse` 包含完整的 `AiMessage`

### 2.4 ChatMemoryProvider 记忆管理

```java
ChatMemoryProvider provider = memoryId -> MessageWindowChatMemory.builder()
    .id(memoryId)
    .maxMessages(maxMessages)
    .chatMemoryStore(chatMemoryStore)
    .build();
```

---

## 三、图执行流程 (LangGraph4j)

### 3.1 图构建

```java
StateGraph<UserSessionState> graph = new StateGraph<>(UserSessionState.SCHEMA, serializer)
    .addNode("Problem", problemInterpreter)
    .addNode("Knowledge", knowledgeBase)
    .addNode("ApiSearch", apiSearch)
    .addNode("SqlRetrieval", sqlRetrieval)
    .addNode("SmallTalk", smallTalk)
    .addNode("IQSRetrieval", iqsRetrieval)
    .addEdge(START, "Problem")                        // 入口 → Problem
    .addConditionalEdges("Problem",                   // Problem 条件路由
        edge_async(UserSessionState::next),
        Map.of("Knowledge","Knowledge", ...))
    .addEdge("Knowledge", END)
    .addEdge("ApiSearch", END)
    // ...
```

### 3.2 编译与执行

```java
CompiledGraph<UserSessionState> compile = graph.compile(
    CompileConfig.builder()
        .checkpointSaver(postgresCheckpointStore)   // 状态持久化（PostgreSQL）
        .releaseThread(false)                        // 手动释放 checkpoint
        .build()
);

for (var output : compile.stream(messages, runnableConfig)) {
    if (output instanceof StreamingOutput streaming) {
        emitter.send(SseEmitter.event().name("message").data(streaming.chunk()));
    }
}
```

### 3.3 执行时序

```
compile.stream() 迭代器
  ├── Problem 节点: apply() → 返回 {next:"ApiSearch", _streaming_messages:gen1}
  │   └── gen1.chunk() → "🔍 正在进行路由节点选择..." → SSE 发送
  │   └── gen1.chunk() → "路由节点完毕" → SSE 发送
  │   └── gen1.chunk() → null (流结束)
  ├── 条件边: next="ApiSearch" → ApiSearch 节点
  ├── ApiSearch 节点: apply() → 返回 {next:"FINISH", _streaming_messages:gen2}
  │   └── gen2.chunk() → "🔍 正在检索数据库..." → SSE 发送
  │   └── gen2.chunk() → LLM 响应逐字输出 → SSE 发送
  │   └── gen2.chunk() → null (流结束)
  └── FINISH → END
      └── emitter.send(done) → 前端关闭连接
```

---

## 四、节点状态信息共享

### 4.1 UserSessionState

所有节点共享 `UserSessionState`：
```java
public class UserSessionState extends MessagesState<ChatMessage> {
    private String threadId;     // 会话 ID
    private String userType;     // 用户类型（null=普通用户，非null=管理员）
    private String next;         // 下一节点（条件路由用）
}
```

### 4.2 StreamingChatGenerator 桥接

每个节点创建自己的 `StreamingChatGenerator`，通过 `_streaming_messages` 传递给图引擎：

```java
StreamingChatGenerator<MessagesState<ChatMessage>> generator = StreamingHelperUtil.bridge(
    "NodeName", userSessionState,
    (chatResponse) -> {
        // mapResult: 流结束时触发，映射到图状态
        chatMemoryStore.addMessages(threadId, chatResponse.aiMessage());
        return Map.of();
    }
);
// 返回给图引擎
return Map.of("next", "FINISH", "_streaming_messages", generator);
```

---

## 五、手动持久化 vs 自动持久化

### 5.1 官方自动持久化 (@MemoryId)

`@MemoryId` 让 AiService 自动管理记忆：

```java
TokenStream chat(@MemoryId String memoryId, @UserMessage String s);
```

**优点**：
- 零代码记忆管理，LLM 自动看到历史上下文
- 无需手动调用 `addMessages`

**缺点**（本项目的实际痛点）：
1. **无法控制保存内容**：保存的是 LLM 原始输出，而非格式化后的结果
2. **中间结果污染**：`SqlDatabaseContentRetriever` 的 SQL 执行中间结果也被保存
3. **无法覆盖**：不能选择性地保存或替换某条消息
4. **重复保存**：与手动保存并存时产生重复消息
5. **与图节点流式输出冲突**：节点的占位文字也被保存

### 5.2 手动持久化（本项目采用）

```java
// 手动保存
chatMemoryStore.addMessages(threadId, AiMessage.from(formattedResult));

// 手动读取历史
String history = chatMemoryStore.getStringLastHistory(threadId, 4);
```

**优点**：
1. **完全控制**：只保存格式化后的最终内容
2. **干净的记忆**：无中间结果、无占位文字
3. **灵活**：可以任意修改保存逻辑

**缺点**：
1. **需要手动编码**：每个节点都要写保存逻辑
2. **容易遗漏**：忘记保存导致记忆丢失
3. **需要封装**：建议抽取出统一的 `executeAndPersist()` 方法

### 5.3 推荐方案

```java
// 封装统一的手动持久化方法
public void executeAndPersist(
    StreamingChatGenerator generator,
    String threadId,
    TokenStream tokenStream,
    Function<ChatResponse, String> formatter
) {
    tokenStream
        .onPartialResponse(generator.handler()::onPartialResponse)
        .onCompleteResponse(resp -> {
            String formatted = formatter.apply(resp);
            chatMemoryStore.addMessages(threadId, AiMessage.from(formatted));
            generator.handler().onCompleteResponse(
                ChatResponse.builder().aiMessage(AiMessage.from(formatted)).build()
            );
        })
        .onError(generator.handler()::onError)
        .start();
}
```

---

## 六、Tool 使用总结

### 6.1 IQS 工具

```java
@Tool("搜索互联网内容")
public String iqsSearch(String query) {
    // 调用阿里云 IQS 搜索引擎
}

// 注入到 AiService
AiServices.builder(IQSRetrievalchat.class)
    .tools(iqsTool)
    .contentRetriever(webSearchRetriever)
    .build();
```

**注意**：
- Tool 类需要注入 `MultipleAiServiceMemory` 才能访问 `chatMemoryStore`
- Tool 方法必须是 public，标注 `@Tool` 注解
- LLM 通过 SystemMessage 指导何时调用 Tool

---

## 七、各节点难点总结

### 7.1 ProblemInterpreterAndRouterChecker（路由）

| 难点 | 解决 |
|---|---|
| LLM 返回非法节点名（如"Chat"） | SystemMessage 末尾显式列出合法值 |
| LLM 因历史记录跳过路由 | SystemMessage 明确"每条消息独立路由" |
| JSON 解析失败（reason 含双引号） | 提示词要求 reason 使用单引号 |

### 7.2 KnowledgeBaseChecker（知识库）

| 难点 | 解决 |
|---|---|
| Milvus 检索为空 | 降级到帮助内容库 → 再降级到 IQS 搜索 |
| 两个知识库需要区分 | 先查手册库，为空再查帮助内容库 |

### 7.3 SqlRetrievalChecker（Text-to-SQL）

| 难点 | 解决 |
|---|---|
| LLM 编造列名（address 而非 detail_address） | SystemMessage 强调使用真实列名 |
| LLM 输出 JSON 而非 SQL | 去掉 SystemMessage 中的 JSON 格式要求 |
| DeepSeek 缓存旧响应 | 重启后端 + 清空对话重新开始 |
| SELECT/DELETE 执行后无反馈 | JDBC 手动执行 + 结果格式化 |
| 序号代指（"删除第三条"） | LLM 从 HTML 卡片中解析 data-id |

### 7.4 ApiSearchRetrievalChecker（API 检索）

| 难点 | 解决 |
|---|---|
| 向量匹配不到 API | LLM 整理历史对话后二次检索 |
| HTTP 调用返回 JSON | QueryJsonOrganizeChat 格式化为 HTML 卡片 |
| `ResponseStream` 双重保存 | 只保留 `mapResult` 中的保存逻辑 |

### 7.5 SmallTalkChecker（闲聊）

| 难点 | 解决 |
|---|---|
| 需要记忆上下文 | 使用 `@MemoryId` 自动管理 |
| 人格设定 | SystemMessage 定义角色和语气 |

### 7.6 GraphService（图引擎）

| 难点 | 解决 |
|---|---|
| SSE 流中 `\n` 被截断 | 后端 JSON 序列化或前端多行合并 |
| 流式 Markdown 渲染不一致 | 前端统一用 `marked.parse()` |
| Checkpoint 未释放 | `finally` 块中调用 `release(runnableConfig)` |
| 节点异步执行导致流提前结束 | ApiSearch 改为同步等待 API 结果 |

---

## 八、架构优化建议

1. **统一手动持久化**：封装 `executeAndPersist()` 方法，去掉 `@MemoryId`
2. **前端占位文字方案**：在后端节点中不发送占位文字，改用前端 loading 状态
3. **SSE 传输优化**：后端用 `ObjectMapper.writeValueAsString(chunk)` 包装每个 chunk
4. **图节点去重**：抽取公共的 generator 创建 + TokenStream 桥接逻辑

---

## 九、总结

LangChain4j + LangGraph4j 组合适合构建复杂的 Agent 工作流应用。核心优势：

- **声明式 AiService**：接口 + 注解定义 LLM 调用，简洁直观
- **图状态管理**：通过 `UserSessionState` + `CheckpointSaver` 实现有状态工作流
- **流式输出**：`TokenStream` + `StreamingChatGenerator` 实现逐字推送

核心挑战：

- **记忆管理**：自动持久化不够灵活，手动持久化需要规范封装
- **流式传输**：SSE 协议对特殊字符处理需要额外注意
- **LLM 不确定性**：需要大量 SystemMessage 调优来控制输出格式
