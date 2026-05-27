package com.fishseedling.platform.myLangChain4j.graph.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
public class AiSearchResult {
    private String title;

    // ✅ 使用 String 接收，避免 URI 解析失败
    private String url;

    private String snippet;
    private String content;
    private Map<String, Object> metadata;
}