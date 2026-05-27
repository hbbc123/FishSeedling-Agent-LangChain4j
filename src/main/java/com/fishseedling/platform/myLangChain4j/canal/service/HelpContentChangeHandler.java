package com.fishseedling.platform.myLangChain4j.canal.service;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.fishseedling.platform.entity.HelpContent;
import com.fishseedling.platform.myLangChain4j.canal.utils.CanalHelpContentMessageParserUtil;
import com.fishseedling.platform.myLangChain4j.milvus.service.MilvusSyncHelpComentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * tb_help_content 表变更处理器
 * 监听帮助内容表的变化，同步到 Milvus 向量数据库
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
@Slf4j
@Component
public class HelpContentChangeHandler implements TableChangeHandler {

    private static final String TABLE_NAME = "tb_help_content";

    private static final Set<String> WATCHED_FIELDS = new HashSet<>(Arrays.asList(
            "title",
            "content",
            "category",
            "status",
            "sort_order"
    ));

    private final MilvusSyncHelpComentService milvusSyncHelpComentService;

    public HelpContentChangeHandler(MilvusSyncHelpComentService milvusSyncHelpComentService) {
        this.milvusSyncHelpComentService = milvusSyncHelpComentService;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public Set<String> getWatchedFields() {
        return WATCHED_FIELDS;
    }

    @Override
    public void handleInsert(CanalEntry.RowChange rowChange) {
        List<HelpContent> helpContents = CanalHelpContentMessageParserUtil.parseEntry(rowChange, true);
        for (HelpContent helpContent : helpContents) {
            log.info("[tb_help_content INSERT] id={}, title={}, category={}, status={}",
                    helpContent.getId(), helpContent.getTitle(), helpContent.getCategory(), helpContent.getStatus());
            if ("enabled".equals(helpContent.getStatus())) {
                milvusSyncHelpComentService.syncToMilvus(helpContent);
            }
        }
    }

    @Override
    public void handleUpdate(CanalEntry.RowChange rowChange, Map<String, Object> beforeData, Map<String, Object> afterData) {
        List<HelpContent> helpContents = CanalHelpContentMessageParserUtil.parseEntry(rowChange, true);
        for (HelpContent helpContent : helpContents) {
            log.info("[tb_help_content UPDATE] id={}, title={}, category={}, status={}",
                    helpContent.getId(), helpContent.getTitle(), helpContent.getCategory(), helpContent.getStatus());
            if ("enabled".equals(helpContent.getStatus())) {
                milvusSyncHelpComentService.syncToMilvus(helpContent);
            } else {
                Object beforeStatus = beforeData.get("status");
                if ("enabled".equals(beforeStatus)) {
                    milvusSyncHelpComentService.deleteFromMilvus(helpContent.getId());
                    log.info("[tb_help_content UPDATE-DISABLED] id={} 已禁用，从 Milvus 删除", helpContent.getId());
                }
            }
        }
    }

    @Override
    public void handleDelete(CanalEntry.RowChange rowChange) {
        List<HelpContent> helpContents = CanalHelpContentMessageParserUtil.parseEntry(rowChange, false);
        for (HelpContent helpContent : helpContents) {
            log.info("[tb_help_content DELETE] id={}, title={}", helpContent.getId(), helpContent.getTitle());
            milvusSyncHelpComentService.deleteFromMilvus(helpContent.getId());
        }
    }
}

