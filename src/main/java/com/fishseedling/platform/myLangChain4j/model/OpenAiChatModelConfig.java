package com.fishseedling.platform.myLangChain4j.model;


import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class OpenAiChatModelConfig {
    @Value("${llm.base-url:127.0.0.1}")
    private String baseUrl;

    @Value("${llm.api-key:11111}")
    private String apiKey;

    @Value("${llm.model-name:example}")
    private String modelName;

    @Value("${llm.log-requests}")
    private boolean logRequests;

    @Value("${llm.log-responses}")
    private boolean logResponses;

    @Bean
    public OpenAiChatModel MyOpenAiChatModel() {
        return  OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
