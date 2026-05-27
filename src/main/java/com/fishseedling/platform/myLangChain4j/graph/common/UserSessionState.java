package com.fishseedling.platform.myLangChain4j.graph.common;

import dev.langchain4j.data.message.ChatMessage;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Map;


/**
 * 状态器 保存各个节点要传递的数据
 *
 *
 * UserSessionState 对象
 * │
 * └── data (Map<String, Object>)
 *     │
 *     ├── "messages" → List<ChatMessage>
 *     │   │
 *     │   ├── [0] ChatMessage (USER)   ← 用户消息
 *     │   ├── [1] ChatMessage (AI)     ← AI消息
 *     │   ├── [2] ChatMessage (USER)   ← 用户消息
 *     │   ├── [3] ChatMessage (AI)     ← AI消息
 *     │   └── [4] ChatMessage (SYSTEM) ← 系统消息
 *     │
 *     ├── "userType" → "vip"           ← 其他业务数据
 *     ├── "errorMsg" → ""              ← 其他业务数据
 *     └── "next" → "agent_name"        ← 其他业务数据
 * **/
public class UserSessionState  extends MessagesState<ChatMessage> {

    /**
     * 构造函数
     * @param initData 初始化数据 Map，包含初始状态值
     */
    public UserSessionState(Map<String, Object> initData) {
        super(initData);  // 调用父类构造函数，初始化消息列表
    }



    /**
     * 获取用户类型
     * @return 返回用户类型userType
     */
    public String getUserType() {
        return value("userType")
                .map(String::valueOf)
                .orElse(null);  // 默认普通用户
    }


    /**
     * 获取失败消息
     * @return 返回用户类型userType
     */
    public String errorMsg() {
        return value("errorMsg")
                .map(String::valueOf)
                .orElse("未知错误");  // 默认普通用户
    }

    /**
     * 获取下一个要执行的 Agent 名称
     * @return 包含下一个 Agent 名称的 Optional 对象（可能为空）
     *
     * 使用场景：监督者 Agent 会将决策结果存入 "next" 字段，
     * 条件边通过读取这个字段来决定路由到哪个子 Agent
     */
    public String next() {
        return (String)value("next").orElse(null);  // 从状态 Map 中获取 "next" 字段的值
    }
    public String getThreadId() {
        return (String)value("threadId").orElse(null);
    }

}