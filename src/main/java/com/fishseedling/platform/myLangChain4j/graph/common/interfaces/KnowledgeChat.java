package com.fishseedling.platform.myLangChain4j.graph.common.interfaces;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.*;

public interface KnowledgeChat {
    @SystemMessage("""
        请将知识库检索到的信息进行整理让用户更好的理解,以知识库为主进行解释还可以拓展知识,只返回整理后的知识即可
        
        返回内容设置设定：统一返回markdown格式
        
        用户发送的问题就是知识库中检索到的内容:
        """)
    TokenStream chat(@UserMessage String msg);
}
