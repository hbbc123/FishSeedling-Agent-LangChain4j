package com.fishseedling.platform.myLangChain4j.graph.pojo.aiResult;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class ProblemInterpreterResult {
    /**
     * 下一个要路由到的工作者名称
     * @Description 注解为 LLM 提供字段说明，帮助 LLM 理解字段含义
     */
    @Description("要路由到的下一个的节点。如果不需要下一节点，路由到FINISH。")
    String next;

    @Description("到FINISH节点的理由是什么")
    String finishMsg;

    @Description("为什么选这个路由")
    String reason;



    @Override
    public String toString() {
        return "路由器{下一个='" + next + "'}";  // 格式化输出，便于调试
    }
}
