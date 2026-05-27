package com.fishseedling.platform.myLangChain4j.llmTodo;

import com.fishseedling.platform.myLangChain4j.graph.common.MultipleAiServiceMemory;
import com.fishseedling.platform.myLangChain4j.graph.common.interfaces.IQSRetrievalchat;
import dev.langchain4j.agent.tool.Tool;
import lombok.Data;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Data
public class IqsTool {
    @Resource
    @Lazy
    MultipleAiServiceMemory multipleAiServiceMemory;

    @Tool("用户要检索的问题")
    String IqsToolSearch(String problem){
        IQSRetrievalchat iqsRetrievalChat = multipleAiServiceMemory.getIQSRetrievalChat();
        return iqsRetrievalChat.chat(problem);
    }
}
