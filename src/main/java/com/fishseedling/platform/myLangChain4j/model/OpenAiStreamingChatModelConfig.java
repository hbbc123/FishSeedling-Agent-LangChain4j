package com.fishseedling.platform.myLangChain4j.model;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class OpenAiStreamingChatModelConfig {
    @Value("${llm.base-url:127.0.0.1}")
    private String baseUrl;

    @Value("${llm.api-key:sk-your-key}")
    private String apiKey;

    @Value("${llm.model-name:example}")
    private String modelName;

    @Value("${llm.log-requests:false}")
    private boolean logRequests;

    @Value("${llm.log-responses:false}")
    private boolean logResponses;


    @Bean
    public OpenAiStreamingChatModel MyOpenAiStreamingChatModel() {


        return   OpenAiStreamingChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .logRequests(logRequests)
                    .logResponses(logResponses)
                    .build();
    }
}
