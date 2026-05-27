package com.fishseedling.platform.myLangChain4j.model.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.internal.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchInformationResult;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
// ... 其他 import

/**
 * 阿里云信息查询服务（IQS）的 LangChain4j 适配器
 */
@Slf4j
public class AliyunIQSWebSearchEngineUtil implements WebSearchEngine {

    private static final String ENDPOINT = "https://cloud-iqs.aliyuncs.com/search/unified";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String engineType;
    private final int maxResults;

    private AliyunIQSWebSearchEngineUtil(Builder builder) {
        this.apiKey = builder.apiKey;
        this.engineType = builder.engineType;
        this.maxResults = builder.maxResults;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public WebSearchResults search(WebSearchRequest webSearchRequest) {
        try {
            // 构建请求体
            String requestBody = buildRequestBody(webSearchRequest.searchTerms());

            Request httpRequest = new Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("IQS API error: " + response.code());
                }

                String responseBody = response.body().string();
                List<WebSearchOrganicResult> results = parseResponse(responseBody);

                // 关键：使用 from() 静态方法创建 WebSearchResults
                return WebSearchResults.from(
                        WebSearchInformationResult.from((long) results.size()),
                        results
                );
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to call IQS API", e);
        }
    }

    private String buildRequestBody(String query) {
        return String.format("""
            {
                "query": "%s",
                "engineType": "%s",
                "contents": {
                    "mainText": false,
                    "summary": false,
                    "rerankScore": true
                },
                "advancedParams": {
                    "numResults": %d
                }
            }
            """, escapeJson(query), engineType, maxResults);
    }

    private List<WebSearchOrganicResult> parseResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode pageItems = root.path("pageItems");

        List<WebSearchOrganicResult> results = new ArrayList<>();

        if (pageItems.isArray()) {
            for (JsonNode item : pageItems) {
                String title = item.path("title").asText();
                String link = item.path("link").asText();
                String snippet = item.path("snippet").asText();
                String content = item.path("content").asText();

                if (title != null && !title.isEmpty() && link != null && !link.isEmpty()) {
                    try {
                        // 参考 Tavily 的实现，使用 from() 方法
                        WebSearchOrganicResult result = WebSearchOrganicResult.from(
                                title,
                                new URI(link),
                                snippet != null ? snippet : "",
                                content != null ? content : null
                        );
                        log.debug("++++++++++++联网检索内容+++++++++++");
                        log.debug(String.valueOf(result));
                        results.add(result);
                    } catch (URISyntaxException e) {
                        System.err.println("无效的 URI: " + link);
                    }
                }
            }
        }

        return results;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static class Builder {
        private String apiKey;
        private String engineType = "LiteAdvanced";
        private int maxResults = 5;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder engineType(String engineType) {
            this.engineType = engineType;
            return this;
        }

        public Builder maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public AliyunIQSWebSearchEngineUtil build() {
            ValidationUtils.ensureNotBlank(apiKey, "apiKey");
            return new AliyunIQSWebSearchEngineUtil(this);
        }
    }
}