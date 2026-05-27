package com.fishseedling.platform.myLangChain4j.milvus.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ApiDefinition {
    private double similarityScore;
    private String interfaceId;
    private String path;
    private String method;
    private String parametersDefinition;
    private String matchedDescription;
}
