package com.fishseedling.platform.myLangChain4j.model;


import com.fishseedling.platform.myLangChain4j.model.utils.DashScopeRerankingModelUtil;
import dev.langchain4j.model.scoring.ScoringModel;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScoringModelConfig {

    @Value("${scoring.api-key:sk-your-key}")
    private String apiKey;

    @Value("${scoring.model-name:qwen3-rerank}")
    private String modelName;

    @Value("${scoring.top-n:5}")
    private Integer topN;

    @Value("${scoring.timeout-seconds:30}")
    private Integer timeoutSeconds;

    @Bean
    public ScoringModel MyScoringModel() {
        // 如果配置文件中没有 api-key，则从 .env 文件读取
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = Dotenv.load().get("AI_KEY");
        }

        return DashScopeRerankingModelUtil.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .topN(topN)
                .timeoutSeconds(timeoutSeconds)
                .build();
    }
}