package com.fishseedling.platform.myLangChain4j.milvus.service;


import com.fishseedling.platform.entity.Handbook;
import com.fishseedling.platform.myLangChain4j.milvus.entity.ApiDefinition;
import com.fishseedling.platform.myLangChain4j.milvus.mapper.HandbookVectorizerUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.filter.comparison.ContainsString;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Milvus 同步服务
 * 负责处理 Canal 同步过来的手册数据
 */
@Slf4j
@Service
public class MilvusSyncHandBookService {

    @Resource
    private HandbookVectorizerUtil handbookVectorizer;

    @Resource
    private MilvusEmbeddingStore apiEmbeddingStore;



    /**
     * 同步到 Milvus（新增或更新）
     * @param handbook 手册实体
     */
    public void syncToMilvus(Handbook handbook) {
        if (handbook == null) {
            log.warn("同步失败：handbook 为空");
            return;
        }

        try {
            // 先删除旧数据（覆盖策略）
            deleteFromMilvus(handbook.getId());

            // 再同步新数据
            handbookVectorizer.syncToVectorStore(handbook);

            log.debug("同步到 Milvus 成功: id={}, title={}", handbook.getId(), handbook.getFishName());
        } catch (Exception e) {
            log.error("同步到 Milvus 失败: id={}, title={}", handbook.getId(), handbook.getFishName(), e);
            // 根据业务需求决定是否抛出异常
            // throw new RuntimeException("Milvus 同步失败", e);
        }
    }

    /**
     * 从 Milvus 删除
     * @param id 手册ID
     */
    public void deleteFromMilvus(Integer id) {
        if (id == null) {
            log.warn("删除失败：id 为空");
            return;
        }

        try {
            handbookVectorizer.deleteFromVectorStore(id);
            log.info("从 Milvus 删除: id={}", id);
        } catch (Exception e) {
            log.error("从 Milvus 删除失败: id={}", id, e);
        }
    }

    /**
     * 批量同步到 Milvus
     * @param handbooks 手册列表
     */
    public void batchSyncToMilvus(List<Handbook> handbooks) {
        if (handbooks == null || handbooks.isEmpty()) {
            log.warn("批量同步失败：handbooks 为空");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (Handbook handbook : handbooks) {
            try {
                syncToMilvus(handbook);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("批量同步失败，id={}", handbook.getId(), e);
            }
        }

        log.info("批量同步完成，成功: {}, 失败: {}", successCount, failCount);
    }

    public List<EmbeddingMatch<TextSegment>> findHandbooks(String userQuery, int topK){
        // 将用户查询转为向量
        String extract = extract(userQuery);
        Embedding queryEmbedding = handbookVectorizer.embeddingModel.embed(userQuery).content();
        log.debug("=====手册知识库  查鱼名=====");
        log.debug(extract);
        ContainsString fishName = new ContainsString("fishName", extract);
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .filter(fishName)
                .queryEmbedding(queryEmbedding)      // 查询向量
                .maxResults(topK)                   // 返回前3个结果
                .minScore(0.7)                   // 最低相似度阈值
                .build();

        // 搜索最相似的文档
        EmbeddingSearchResult<TextSegment> matches = handbookVectorizer.handbookEmbeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches1 = matches.matches();
        return matches1;
    }


    /**
     * 从查询中提取鱼名
     * @param query 用户查询
     * @return 鱼名，如果没有则返回 null
     */
    public  String extract(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }

        // 匹配：任意文字 + "鱼"，取"鱼"前面的词
        Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5]+鱼)");
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    public List<ApiDefinition> findApiPath(String userQuery, int topK) {
            // 将用户查询转为向量
            Embedding queryEmbedding = handbookVectorizer.embeddingModel.embed(userQuery).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)      // 查询向量
                    .maxResults(topK)                   // 返回前3个结果
                    .minScore(0.7)                   // 最低相似度阈值
                    .build();

            // 搜索最相似的文档
            EmbeddingSearchResult<TextSegment> matches = apiEmbeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches1 = matches.matches();
            log.debug("=====api库  查api信息=====");

            ArrayList<ApiDefinition> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches1) {
                TextSegment segment = match.embedded();
                Map<String, Object> metadata = segment.metadata().toMap();

                results.add(new ApiDefinition(
                        match.score(),
                        String.valueOf(metadata.get("interfaceId")),
                        String.valueOf(metadata.get("path")),
                        String.valueOf(metadata.get("method")),
                        String.valueOf(metadata.get("parameters")),
                        segment.text()
                ));
            }
            log.debug(String.valueOf(results));
            return results;
    }

}