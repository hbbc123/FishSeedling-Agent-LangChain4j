package com.fishseedling.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.entity.HelpContent;
import com.fishseedling.platform.mapper.HelpContentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 帮助内容服务类
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
@Service
public class HelpContentService {

    @Resource
    private HelpContentMapper helpContentMapper;

    /**
     * 分页查询（管理端）
     */
    public PageResult<HelpContent> getHelpContentByPage(Integer current, Integer size, String category, String keyword) {
        Page<HelpContent> page = new Page<>(current, size);
        LambdaQueryWrapper<HelpContent> wrapper = new LambdaQueryWrapper<>();

        if (category != null && !category.isEmpty()) {
            wrapper.eq(HelpContent::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(HelpContent::getTitle, keyword)
                    .or().like(HelpContent::getContent, keyword));
        }
        wrapper.orderByAsc(HelpContent::getSortOrder);

        Page<HelpContent> result = helpContentMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 获取所有启用的帮助内容（前台用）
     */
    public List<HelpContent> getAllEnabled() {
        return helpContentMapper.selectAllEnabled();
    }

    /**
     * 按分类获取启用的帮助内容（前台用）
     */
    public List<HelpContent> getByCategoryEnabled(String category) {
        return helpContentMapper.selectByCategoryEnabled(category);
    }

    /**
     * 按分类分组获取启用的帮助内容
     */
    public Map<String, List<HelpContent>> getGroupedByCategory() {
        List<HelpContent> all = getAllEnabled();
        Map<String, List<HelpContent>> grouped = new java.util.LinkedHashMap<>();
        for (HelpContent hc : all) {
            grouped.computeIfAbsent(hc.getCategory(), k -> new java.util.ArrayList<>()).add(hc);
        }
        return grouped;
    }

    /**
     * 获取详情
     */
    public HelpContent getById(Long id) {
        return helpContentMapper.selectById(id);
    }

    /**
     * 新增
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean create(HelpContent helpContent) {
        if (helpContent.getStatus() == null) {
            helpContent.setStatus("enabled");
        }
        if (helpContent.getSortOrder() == null) {
            helpContent.setSortOrder(0);
        }
        return helpContentMapper.insert(helpContent) > 0;
    }

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean update(HelpContent helpContent) {
        return helpContentMapper.updateById(helpContent) > 0;
    }

    /**
     * 删除
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return helpContentMapper.deleteById(id) > 0;
    }
}
