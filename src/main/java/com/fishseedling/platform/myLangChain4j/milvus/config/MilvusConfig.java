package com.fishseedling.platform.myLangChain4j.milvus.config;

import com.fishseedling.platform.entity.HelpContent;
import com.fishseedling.platform.myLangChain4j.milvus.entity.ApiDefinition;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class MilvusConfig {
    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:11111}")
    private int port;


    @Value("${milvus.dimension:384}")
    private int dimension;

    @Value("${milvus.username:root}")
    private String username;

    @Value("${milvus.password:Milvus}")
    private String password;




    @Bean("handbookEmbeddingStore")
    public EmbeddingStore handbookEmbeddingStore() {
        return  MilvusEmbeddingStore.builder()
                .collectionName("handbookEmbeddingStore")
                .host(host)                    // Milvus 服务主机
                .port(port)                          // Milvus 服务端口（gRPC）
                .dimension(dimension)                       // 向量维度（必须与嵌入模型匹配）
                .username(username)           // ⭐ 添加用户名
                .password(password)
                .build();
    }


    /**
     * API定义向量库（专门存储API元数据）
     */
    @Bean("apiEmbeddingStore")
    public EmbeddingStore apiEmbeddingStore() {
        return MilvusEmbeddingStore.builder()
                .collectionName("apiEmbeddingStore")
                .host(host)
                .port(port)
                .dimension(dimension)
                .username(username)
                .password(password)
                .build();
    }


    /**
     * 使用帮助库
     */
    @Bean("helpContentEmbeddingStore")
    public EmbeddingStore helpContentEmbeddingStore() {
        return MilvusEmbeddingStore.builder()
                .collectionName("helpContentEmbeddingStore")
                .host(host)
                .port(port)
                .dimension(dimension)
                .username(username)
                .password(password)
                .build();
    }
}
