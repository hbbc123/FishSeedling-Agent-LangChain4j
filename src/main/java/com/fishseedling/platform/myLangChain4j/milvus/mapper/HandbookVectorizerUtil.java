package com.fishseedling.platform.myLangChain4j.milvus.mapper;


import com.fishseedling.platform.entity.Handbook;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import dev.langchain4j.store.embedding.filter.Filter;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 手册向量化同步工具类
 * 负责将 Handbook 实体转换为 Document 并同步到 Milvus
 */
@Slf4j
@Component
public class HandbookVectorizerUtil {


    @Resource
    public EmbeddingModel embeddingModel;

    @Resource(name = "handbookEmbeddingStore")
    public EmbeddingStore handbookEmbeddingStore;




    // 分片配置
    private static final int CHUNK_SIZE = 300;
    private static final int OVERLAP = 20;
    private static final int MAX_CONTENT_LENGTH = 5000;

    /**
     * 将手册同步到向量数据库
     * @param handbook 手册实体
     */
    public void syncToVectorStore(Handbook handbook) {
        if (handbook == null || handbook.getId() == null) {
            log.warn("同步失败：handbook 或 id 为空");
            return;
        }

        if (handbook.getIsPublished() != 1) {
            log.info("手册未发布，跳过同步: id={}, isPublished={}",
                    handbook.getId(), handbook.getIsPublished());
            return;
        }

        try {
            // 1. 构建文档内容
            String documentContent = buildDocumentContent(handbook);
            if (documentContent == null || documentContent.trim().isEmpty()) {
                log.warn("手册内容为空，跳过同步: id={}", handbook.getId());
                return;
            }

            // 2. 构建元数据
            Metadata metadata = buildMetadata(handbook);

            // 3. 创建 Document
            Document document = Document.from(documentContent,metadata);

            // 4. 使用 Ingestor 进行分片和向量化
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(CHUNK_SIZE, OVERLAP))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(handbookEmbeddingStore)
                    .build();

            ingestor.ingest(document);

            log.info("手册已同步到向量库: id={}, 标题={}", handbook.getId(), handbook.getFishName());

        } catch (Exception e) {
            log.error("同步到向量库失败，id={}, 标题={}", handbook.getId(), handbook.getFishName(), e);
            throw new RuntimeException("向量化同步失败", e);
        }
    }

    /**
     * 构建文档内容
     * 将手册的关键信息组合成适合向量化的文本
     */
    private String buildDocumentContent(Handbook handbook) {
        StringBuilder content = new StringBuilder();

        // 标题（重要，权重高）
        if (handbook.getFishName() != null && !handbook.getFishName().isEmpty()) {
            content.append("【名称】").append(handbook.getFishName()).append("\n");
        }

        // 别名
        if (handbook.getFishAlias() != null && !handbook.getFishAlias().isEmpty()) {
            content.append("【别名】").append(handbook.getFishAlias()).append("\n");
        }

        // 简介
        if (handbook.getBriefIntro() != null && !handbook.getBriefIntro().isEmpty()) {
            content.append("【简介】").append(handbook.getBriefIntro()).append("\n");
        }

        // 难度
        if (handbook.getDifficulty() != null) {
            content.append("【养殖难度】").append(parseDifficulty(handbook.getDifficulty())).append("\n");
        }

        // 分类
        if (handbook.getCategory() != null && !handbook.getCategory().isEmpty()) {
            content.append("【分类】").append(handbook.getCategory()).append("\n");
        }

        // 正文内容（清洗HTML）
        if (handbook.getContent() != null && !handbook.getContent().isEmpty()) {
            String cleanContent = cleanHtml(handbook.getContent());
            content.append("【详细内容】\n").append(cleanContent);
        }

        String result = content.toString();

        // 限制总长度，避免过长
        if (result.length() > MAX_CONTENT_LENGTH) {
            result = result.substring(0, MAX_CONTENT_LENGTH);
        }

        return result;
    }

    /**
     * 解析难度等级
     */
    private String parseDifficulty(Object difficulty) {
        if (difficulty == null) return "未知";
        int level;
        if (difficulty instanceof Integer) {
            level = (Integer) difficulty;
        } else {
            try {
                level = Integer.parseInt(difficulty.toString());
            } catch (NumberFormatException e) {
                return "未知";
            }
        }
        switch (level) {
            case 1: return "简单";
            case 2: return "中等";
            case 3: return "困难";
            default: return "未知";
        }
    }

    /**
     * 清洗 HTML 内容
     */
    private String cleanHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        String text = html;

        // 移除 script 和 style
        text = text.replaceAll("(?i)<script[^>]*>.*?</script>", " ");
        text = text.replaceAll("(?i)<style[^>]*>.*?</style>", " ");

        // 移除 HTML 标签
        text = text.replaceAll("<[^>]+>", " ");

        // 处理 HTML 实体
        text = text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"");

        // 标点符号处理，增强语义分隔
        text = text.replaceAll("([：、；])", "。");
        text = text.replaceAll("([。！？])", "$1\n");

        // 处理列表项
        text = text.replaceAll("(\\d+\\.)\\s*", "\n$1 ");
        text = text.replaceAll("[·•●○◆■□]", "\n• ");

        // 清理多余空白
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    /**
     * 构建元数据
     * 用于存储手册的结构化信息，方便检索后溯源
     */
    private Metadata buildMetadata(Handbook handbook) {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("id", String.valueOf(handbook.getId()));
        metadataMap.put("fishName", handbook.getFishName() != null ? handbook.getFishName()+"、"+handbook.getFishAlias() : "");
        metadataMap.put("fishAlias", handbook.getFishAlias() != null ? handbook.getFishAlias()+"、"+handbook.getFishName() : "");
        metadataMap.put("category", handbook.getCategory() != null ? handbook.getCategory() : "");
        metadataMap.put("difficulty", handbook.getDifficulty() != null ? String.valueOf(handbook.getDifficulty()) : "");
        metadataMap.put("isPublished", String.valueOf(handbook.getIsPublished()));
        metadataMap.put("sortOrder", handbook.getSortOrder() != null ? String.valueOf(handbook.getSortOrder()) : "0");
        metadataMap.put("viewCount", handbook.getViewCount() != null ? String.valueOf(handbook.getViewCount()) : "0");

        // 可选：添加时间字段
        if (handbook.getCreateTime() != null) {
            metadataMap.put("createTime", handbook.getCreateTime().toString());
        }
        if (handbook.getUpdateTime() != null) {
            metadataMap.put("updateTime", handbook.getUpdateTime().toString());
        }

        return Metadata.from(metadataMap);
    }

    /**
     * 从 Milvus 删除指定业务 ID 的所有向量数据
     * 使用 Filter 根据元数据中的 id 字段进行删除
     *
     * @param businessId 业务ID（handbook.id）
     */
    public void deleteFromVectorStore(Integer businessId) {
        if (businessId == null) {
            log.warn("删除失败：id 为空");
            return;
        }
        try {
            // 使用 Filter 构建删除条件
            // 注意：这里的 "id" 是 Metadata 中设置的字段名，对应 handbook.getId()
            Filter filter = new IsEqualTo("id", String.valueOf(businessId));

            // 执行删除：删除所有元数据中 id 字段匹配的记录
            handbookEmbeddingStore.removeAll(filter);
            log.info("从 Milvus 删除成功: businessId={}", businessId);

        } catch (Exception e) {
            log.error("从 Milvus 删除失败: businessId={}", businessId, e);
            throw new RuntimeException("Milvus 删除失败", e);
        }
    }



}