package com.fishseedling.platform.myLangChain4j.model;

import com.fishseedling.platform.myLangChain4j.model.utils.AliyunIQSWebSearchEngineUtil;
import dev.langchain4j.web.search.WebSearchEngine;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSearchEngineConfig {

    @Value("${websearch.api-key:apiKey}")
    private String apiKey;

    @Value("${websearch.engine-type:LiteAdvanced}")
    private String engineType;

    @Value("${websearch.max-results:2}")
    private Integer maxResults;

    @Bean
    public WebSearchEngine MyWebSearchEngine() {
        // 如果配置文件中没有 api-key，则从 .env 文件读取
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = Dotenv.load().get("ALIYUN_IQS_API_KEY");
        }

        return AliyunIQSWebSearchEngineUtil.builder()
                .apiKey(apiKey)
                .engineType(engineType)
                .maxResults(maxResults)
                .build();
    }
}