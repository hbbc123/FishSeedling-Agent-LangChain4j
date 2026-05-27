package com.fishseedling.platform.myLangChain4j.canal.service;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.fishseedling.platform.entity.Handbook;
import com.fishseedling.platform.myLangChain4j.canal.utils.CanalHandBookMessageParserUtil;
import com.fishseedling.platform.myLangChain4j.milvus.service.MilvusSyncHandBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * tb_handbooks 表变更处理器
 * 监听养殖手册表的变化，同步到 Milvus 向量数据库
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
@Slf4j
@Component
public class HandbookChangeHandler implements TableChangeHandler {

    private static final String TABLE_NAME = "tb_handbooks";

    private static final Set<String> WATCHED_FIELDS = new HashSet<>(Arrays.asList(
            "fish_name",      // 对应 Handbook.fishName
            "fish_alias",     // 对应 Handbook.fishAlias
            "brief_intro",    // 对应 Handbook.briefIntro
            "difficulty",     // 对应 Handbook.difficulty
            "content",        // 对应 Handbook.content
            "is_published"    // 对应 Handbook.isPublished
    ));

    private final MilvusSyncHandBookService milvusSyncService;

    public HandbookChangeHandler(MilvusSyncHandBookService milvusSyncService) {
        this.milvusSyncService = milvusSyncService;
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
        List<Handbook> handbooks = CanalHandBookMessageParserUtil.parseEntry(rowChange, true);
        for (Handbook handbook : handbooks) {
            log.info("[tb_handbooks INSERT] id={}, fishName={}, isPublished={}",
                    handbook.getId(), handbook.getFishName(), handbook.getIsPublished());
            if (handbook.getIsPublished() == 1) {
                milvusSyncService.syncToMilvus(handbook);
            }
        }
    }

    @Override
    public void handleUpdate(CanalEntry.RowChange rowChange, Map<String, Object> beforeData, Map<String, Object> afterData) {
        List<Handbook> handbooks = CanalHandBookMessageParserUtil.parseEntry(rowChange, true);
        for (Handbook handbook : handbooks) {
            log.info("[tb_handbooks UPDATE] id={}, fishName={}, isPublished={}",
                    handbook.getId(), handbook.getFishName(), handbook.getIsPublished());
            if (handbook.getIsPublished() == 1) {
                milvusSyncService.syncToMilvus(handbook);
            } else {
                Integer beforePublished = (Integer) beforeData.get("is_published");
                if (beforePublished != null && beforePublished == 1) {
                    milvusSyncService.deleteFromMilvus(handbook.getId());
                    log.info("[tb_handbooks UPDATE-UNPUBLISH] id={} 已下架，从 Milvus 删除", handbook.getId());
                }
            }
        }
    }

    @Override
    public void handleDelete(CanalEntry.RowChange rowChange) {
        List<Handbook> handbooks = CanalHandBookMessageParserUtil.parseEntry(rowChange, false);
        for (Handbook handbook : handbooks) {
            log.info("[tb_handbooks DELETE] id={}, fishName={}", handbook.getId(), handbook.getFishName());
            milvusSyncService.deleteFromMilvus(handbook.getId());
        }
    }
}
