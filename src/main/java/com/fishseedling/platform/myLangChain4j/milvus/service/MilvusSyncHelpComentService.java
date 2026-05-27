package com.fishseedling.platform.myLangChain4j.milvus.service;

import com.fishseedling.platform.entity.HelpContent;
import com.fishseedling.platform.myLangChain4j.milvus.mapper.HelpContentVectorizerUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.comparison.ContainsString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * Milvus 同步服务 - HelpContent 专用
 * 负责处理 Canal 同步过来的帮助内容数据
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
@Slf4j
@Service
public class MilvusSyncHelpComentService {

    @Resource
    private HelpContentVectorizerUtil helpContentVectorizer;

    /**
     * 同步到 Milvus（新增或更新）
     */
    public void syncToMilvus(HelpContent helpContent) {
        if (helpContent == null) {
            log.warn("同步失败：helpContent 为空");
            return;
        }

        try {
            // 先删除旧数据（覆盖策略）
            deleteFromMilvus(helpContent.getId());
            // 再同步新数据
            helpContentVectorizer.syncToVectorStore(helpContent);
            log.info("帮助内容同步到 Milvus 成功: id={}, title={}", helpContent.getId(), helpContent.getTitle());
        } catch (Exception e) {
            log.error("帮助内容同步到 Milvus 失败: id={}, title={}", helpContent.getId(), helpContent.getTitle(), e);
        }
    }

    /**
     * 从 Milvus 删除
     */
    public void deleteFromMilvus(Long id) {
        if (id == null) {
            log.warn("删除失败：id 为空");
            return;
        }
        try {
            helpContentVectorizer.deleteFromVectorStore(id);
            log.info("从 Milvus 删除帮助内容: id={}", id);
        } catch (Exception e) {
            log.error("从 Milvus 删除帮助内容失败: id={}", id, e);
        }
    }


    public List<EmbeddingMatch<TextSegment>> findHandbooks(String userQuery, int topK){
        // 将用户查询转为向量
        Embedding queryEmbedding = helpContentVectorizer.embeddingModel.embed(userQuery).content();
        log.debug("=====帮助手册知识库=====");
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)      // 查询向量
                .maxResults(topK)                   // 返回前3个结果
                .minScore(0.8)                   // 最低相似度阈值
                .build();

        // 搜索最相似的文档
        EmbeddingSearchResult<TextSegment> matches = helpContentVectorizer.helpContentEmbeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches1 = matches.matches();
        return matches1;
    }
}

