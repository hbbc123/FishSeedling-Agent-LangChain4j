package com.fishseedling.platform.myLangChain4j.milvus.mapper;

import com.fishseedling.platform.entity.HelpContent;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 帮助内容向量化同步工具类
 * 负责将 HelpContent 实体转换为 Document 并同步到 Milvus
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
@Slf4j
@Component
public class HelpContentVectorizerUtil {

    @Resource
    public EmbeddingModel embeddingModel;

    @Resource(name = "helpContentEmbeddingStore")
    public EmbeddingStore helpContentEmbeddingStore;

    private static final int CHUNK_SIZE = 300;
    private static final int OVERLAP = 20;
    private static final int MAX_CONTENT_LENGTH = 5000;

    /**
     * 将帮助内容同步到向量数据库
     */
    public void syncToVectorStore(HelpContent helpContent) {
        if (helpContent == null || helpContent.getId() == null) {
            log.warn("同步失败：helpContent 或 id 为空");
            return;
        }

        if (!"enabled".equals(helpContent.getStatus())) {
            log.info("帮助内容已禁用，跳过同步: id={}, title={}", helpContent.getId(), helpContent.getTitle());
            return;
        }

        try {
            String documentContent = buildDocumentContent(helpContent);
            if (documentContent == null || documentContent.trim().isEmpty()) {
                log.warn("帮助内容为空，跳过同步: id={}", helpContent.getId());
                return;
            }

            Metadata metadata = buildMetadata(helpContent);
            Document document = Document.from(documentContent, metadata);

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(CHUNK_SIZE, OVERLAP))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(helpContentEmbeddingStore)
                    .build();

            ingestor.ingest(document);
            log.info("帮助内容已同步到向量库: id={}, title={}, category={}",
                    helpContent.getId(), helpContent.getTitle(), helpContent.getCategory());

        } catch (Exception e) {
            log.error("同步到向量库失败，id={}, title={}", helpContent.getId(), helpContent.getTitle(), e);
            throw new RuntimeException("向量化同步失败", e);
        }
    }

    private String buildDocumentContent(HelpContent helpContent) {
        StringBuilder content = new StringBuilder();
        if (helpContent.getCategory() != null) {
            content.append("【分类】").append(parseCategory(helpContent.getCategory())).append("\n");
        }
        if (helpContent.getTitle() != null && !helpContent.getTitle().isEmpty()) {
            content.append("【标题】").append(helpContent.getTitle()).append("\n");
        }
        if (helpContent.getContent() != null && !helpContent.getContent().isEmpty()) {
            String cleanContent = cleanHtml(helpContent.getContent());
            content.append("【内容】\n").append(cleanContent);
        }
        String result = content.toString();
        if (result.length() > MAX_CONTENT_LENGTH) {
            result = result.substring(0, MAX_CONTENT_LENGTH);
        }
        return result;
    }

    private String parseCategory(String category) {
        if (category == null) return "未知";
        switch (category) {
            case "quick-start": return "快速入门";
            case "publish-guide": return "发布指南";
            case "safety-tips": return "安全须知";
            case "contact-us": return "联系客服";
            default: return category;
        }
    }

    private String cleanHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        String text = html;
        text = text.replaceAll("(?i)<script[^>]*>.*?</script>", " ");
        text = text.replaceAll("(?i)<style[^>]*>.*?</style>", " ");
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&quot;", "\"");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private Metadata buildMetadata(HelpContent helpContent) {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("id", String.valueOf(helpContent.getId()));
        metadataMap.put("helpContentId", String.valueOf(helpContent.getId()));
        metadataMap.put("title", helpContent.getTitle() != null ? helpContent.getTitle() : "");
        metadataMap.put("category", helpContent.getCategory() != null ? helpContent.getCategory() : "");
        metadataMap.put("status", helpContent.getStatus() != null ? helpContent.getStatus() : "");
        metadataMap.put("sortOrder", helpContent.getSortOrder() != null ? String.valueOf(helpContent.getSortOrder()) : "0");
        if (helpContent.getCreateTime() != null) metadataMap.put("createTime", helpContent.getCreateTime().toString());
        if (helpContent.getUpdateTime() != null) metadataMap.put("updateTime", helpContent.getUpdateTime().toString());
        return Metadata.from(metadataMap);
    }

    /**
     * 从 Milvus 删除指定业务 ID 的所有向量数据
     */
    public void deleteFromVectorStore(Long businessId) {
        if (businessId == null) {
            log.warn("删除失败：id 为空");
            return;
        }
        try {
            Filter filter = new IsEqualTo("helpContentId", String.valueOf(businessId));
            helpContentEmbeddingStore.removeAll(filter);
            log.info("从 Milvus 删除成功: businessId={}", businessId);
        } catch (Exception e) {
            log.error("从 Milvus 删除失败: businessId={}", businessId, e);
            throw new RuntimeException("Milvus 删除失败", e);
        }
    }
}

