package com.fishseedling.platform.myLangChain4j.model;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingModelConfig {

    @Value("${embedding.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${embedding.model-name:qwen3-embedding:0.6b}")
    private String modelName;

    @Bean
    public EmbeddingModel MyEmbeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}