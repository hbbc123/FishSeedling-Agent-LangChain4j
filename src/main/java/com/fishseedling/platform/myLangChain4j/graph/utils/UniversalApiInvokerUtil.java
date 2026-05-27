package com.fishseedling.platform.myLangChain4j.graph.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishseedling.platform.util.YamlConfigUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UniversalApiInvokerUtil {

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String BASE_URL = YamlConfigUtil.getFullUrl(); // 从配置获取

    /**
     * 通用请求执行器
     * @param llmResult LLM返回的结果，需要包含 method, path, params
     */
    public static String execute(Map<String, Object> llmResult,String uri) throws Exception {
        String method = (String) llmResult.get("method");
        String path = (String) llmResult.get("path");
        Map<String, Object> params = (Map<String, Object>) llmResult.get("params");

        if (method == null || path == null) {
            throw new IllegalArgumentException("LLM返回结果必须包含method和path");
        }

        String fullUrl = BASE_URL + path;

        if ("GET".equalsIgnoreCase(method)) {
            return doGet(fullUrl, params);
        } else if ("POST".equalsIgnoreCase(method)) {
            return doPost(fullUrl, params);
        } else if ("PUT".equalsIgnoreCase(method)) {
            return doPut(fullUrl, params);
        } else if ("DELETE".equalsIgnoreCase(method)) {
            return doDelete(fullUrl, params);
        } else {
            throw new UnsupportedOperationException("不支持的请求方法: " + method);
        }
    }

    private static String doGet(String url, Map<String, Object> params) throws Exception {
        if (params != null && !params.isEmpty()) {
            StringBuilder queryString = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
            }
            url = url + "?" + queryString;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String doPost(String url, Map<String, Object> params) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(params);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String doPut(String url, Map<String, Object> params) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(params);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String doDelete(String url, Map<String, Object> params) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));

        if (params != null && !params.isEmpty()) {
            String jsonBody = objectMapper.writeValueAsString(params);
            builder.method("DELETE", HttpRequest.BodyPublishers.ofString(jsonBody));
            builder.header("Content-Type", "application/json");
        } else {
            builder.DELETE();
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}