package com.fishseedling.platform.myLangChain4j.model.utils;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云百炼 DashScope 重排序模型
 * 文档：https://help.aliyun.com/zh/model-studio/rerank-api
 */
@Slf4j
public class DashScopeRerankingModelUtil implements ScoringModel {

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final int topN;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    private DashScopeRerankingModelUtil(Builder builder) {
        this.apiKey = builder.apiKey;
        this.modelName = builder.modelName;
        this.baseUrl = builder.baseUrl;
        this.topN = builder.topN;
        this.mapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(builder.timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(builder.timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;
        private String modelName = "qwen3-rerank";
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-api/v1/reranks";
        private int topN = 5;
        private int timeoutSeconds = 60;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder topN(int topN) {
            this.topN = topN;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public DashScopeRerankingModelUtil build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("apiKey is required");
            }
            return new DashScopeRerankingModelUtil(this);
        }
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        try {
            // 1. 提取文档文本
            List<String> documents = new ArrayList<>();
            for (TextSegment segment : segments) {
                documents.add(segment.text());
            }

            // 2. 构建请求体
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    buildRequestBody(query, documents)
            );

            // 3. 构建 HTTP 请求
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            // 4. 执行请求
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    throw new RuntimeException("DashScope API 调用失败: " + response.code() + ", " + errorBody);
                }

                String responseBody = response.body().string();
                return parseScores(responseBody, segments.size());
            }

        } catch (IOException e) {
            throw new RuntimeException("DashScope 请求异常: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String query, List<String> documents) {
        try {
            // 构建 documents 数组的 JSON
            log.debug("==========重排序前内容-==========");
            StringBuilder docsJson = new StringBuilder("[");
            for (int i = 0; i < documents.size(); i++) {
                if (i > 0) docsJson.append(",");
                docsJson.append("\"").append(escapeJson(documents.get(i))).append("\"");
                log.debug("=====索引"+i+"===="+documents.get(i));
            }
            docsJson.append("]");


            return String.format("""
                {
                    "model": "%s",
                    "query": "%s",
                    "documents": %s,
                    "top_n": %d
                }
                """,
                    escapeJson(modelName),
                    escapeJson(query),
                    docsJson.toString(),
                    topN
            );
        } catch (Exception e) {
            throw new RuntimeException("构建请求体失败", e);
        }
    }

    private Response<List<Double>> parseScores(String responseBody, int expectedSize) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode results = root.get("results");
            log.debug("results内容为:");
            log.debug(results.toString());

            // 初始化所有分数为 0.0
            double[] scores = new double[expectedSize];
            for (int i = 0; i < expectedSize; i++) {
                scores[i] = 0.0;
            }

            // 存储每个索引对应的分数（用于后续找最高分）
            Map<Integer, Double> scoreMap = new HashMap<>();

            // 填充有结果的分数
            if (results != null && results.isArray()) {
                for (JsonNode result : results) {
                    int index = result.get("index").asInt();
                    double relevanceScore = result.get("relevance_score").asDouble();
                    if (index >= 0 && index < expectedSize) {
                        scores[index] = relevanceScore;
                        scoreMap.put(index, relevanceScore);
                    }
                }
            }

            // 找出最高分索引和分数
            int maxIndex = -1;
            double maxScore = -1.0;
            for (Map.Entry<Integer, Double> entry : scoreMap.entrySet()) {
                if (entry.getValue() > maxScore) {
                    maxScore = entry.getValue();
                    maxIndex = entry.getKey();
                }
            }

            // ========== 打印最高分信息 ==========
            if (maxIndex != -1) {
                log.debug("========== 重排序最高分 ==========");
                log.debug("最高分索引: " + maxIndex);
                log.debug("最高分分数: " + maxScore);
                // 注意：这里只有分数，没有文档内容
                // 因为你的 results 中只包含 index 和 relevance_score
                log.debug("================================");
            } else {
                log.debug("未找到有效分数");
            }

            // 转换为 List<Double>
            List<Double> scoreList = new ArrayList<>();
            for (double s : scores) {
                scoreList.add(s);
            }

            return Response.from(scoreList);

        } catch (Exception e) {
            throw new RuntimeException("解析响应失败: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}