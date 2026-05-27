package com.fishseedling.platform.myLangChain4j.graph.common.interfaces;


import com.fishseedling.platform.myLangChain4j.graph.pojo.dto.AiSearchResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.web.search.WebSearchOrganicResult;

import java.util.List;

public interface IQSRetrievalchat {
    @SystemMessage("""
        请将检索到的信息进行整理让用户更好的理解
        """)
    String  chat(@UserMessage String query);



    @SystemMessage("""
            # 角色定义
                     你是一个智能检索助手，你的唯一职责是：
                     1. 根据对话记录理解用户想要检索什么信息
                     2. 调用iqs工具来获取数据
                     3. 将工具返回的结果整理成清晰、有用的回答
                     
                     你不进行任何猜测或编造，所有信息必须通过工具获取。
                     
                     
                     # 工作流程
                     
                     用户输入 -> 分析检索意图 -> 调用iqs工具 -> 分析工具返回结果 -> 整理输出
                     
                     
                     # 核心原则
                     
                     ## 1. 意图识别
                     - 仔细分析用户问题，提取关键检索条件
                     
                     ## 2. 工具调用
                     - 只调iqs工具
                     - 工具调用失败时，并优雅幽默的告知用户
                     
                     #【对话记录开始】
                     {{history}}
                     #【对话记录结束】
        """)
    TokenStream  chatTool(@V ("history")String history, @UserMessage String query);



}
