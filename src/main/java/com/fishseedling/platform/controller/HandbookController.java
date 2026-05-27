package com.fishseedling.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.entity.Handbook;
import com.fishseedling.platform.mapper.HandbookMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 养殖手册控制器
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/handbook")
public class HandbookController {

    @Resource
    private HandbookMapper handbookMapper;

    /**
     * 获取养殖手册列表（分页）
     */
    @GetMapping("/list")
    public Result<?> getList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        Page<Handbook> page = new Page<>(current, size);
        LambdaQueryWrapper<Handbook> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询已发布的
        wrapper.eq(Handbook::getIsPublished, true);
        
        // 分类筛选
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Handbook::getCategory, category);
        }
        
        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Handbook::getFishName, keyword)
                    .or().like(Handbook::getFishAlias, keyword)
                    .or().like(Handbook::getBriefIntro, keyword));
        }
        
        // 排序
        wrapper.orderByAsc(Handbook::getSortOrder)
                .orderByDesc(Handbook::getViewCount);
        
        Page<Handbook> result = handbookMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取养殖手册详情
     */
    @GetMapping("/detail/{id}")
    public Result<?> getDetail(@PathVariable Integer id) {
        Handbook handbook = handbookMapper.selectById(id);
        if (handbook == null || handbook.getIsPublished()==0) {
            return Result.error("手册不存在或未发布");
        }
        
        // 增加浏览次数
        handbook.setViewCount(handbook.getViewCount() + 1);
        handbookMapper.updateById(handbook);
        
        return Result.success(handbook);
    }

    /**
     * 获取热门手册
     */
    @GetMapping("/hot")
    public Result<?> getHotHandbooks(@RequestParam(defaultValue = "6") Integer limit) {
        LambdaQueryWrapper<Handbook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Handbook::getIsPublished, true)
                .orderByDesc(Handbook::getViewCount)
                .last("LIMIT " + limit);
        
        List<Handbook> list = handbookMapper.selectList(wrapper);
        return Result.success(list);
    }

    /**
     * 按分类获取手册
     */
    @GetMapping("/category")
    public Result<?> getByCategory() {
        Map<String, Object> result = new HashMap<>();
        
        String[] categories = {"freshwater", "saltwater", "shrimp_crab", "special"};
        String[] categoryNames = {"淡水鱼类", "海水鱼类", "虾蟹类", "特种水产"};
        
        for (int i = 0; i < categories.length; i++) {
            LambdaQueryWrapper<Handbook> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Handbook::getIsPublished, true)
                    .eq(Handbook::getCategory, categories[i])
                    .orderByAsc(Handbook::getSortOrder)
                    .last("LIMIT 4");
            
            List<Handbook> list = handbookMapper.selectList(wrapper);
            result.put(categories[i], list);
        }
        
        return Result.success(result);
    }

    // ==================== 管理端接口 ====================

    /**
     * 管理端 - 获取手册列表（包含未发布的）
     */
    @GetMapping("/admin/list")
    public Result<?> getAdminList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isPublished
    ) {
        Page<Handbook> page = new Page<>(current, size);
        LambdaQueryWrapper<Handbook> wrapper = new LambdaQueryWrapper<>();
        
        // 分类筛选
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Handbook::getCategory, category);
        }
        
        // 发布状态筛选
        if (isPublished != null) {
            wrapper.eq(Handbook::getIsPublished, isPublished);
        }
        
        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Handbook::getFishName, keyword)
                    .or().like(Handbook::getFishAlias, keyword));
        }
        
        // 排序
        wrapper.orderByDesc(Handbook::getCreateTime);
        
        Page<Handbook> result = handbookMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    /**
     * 管理端 - 获取手册详情（包含未发布的）
     */
    @GetMapping("/admin/detail/{id}")
    public Result<?> getAdminDetail(@PathVariable Integer id) {
        Handbook handbook = handbookMapper.selectById(id);
        if (handbook == null) {
            return Result.error("手册不存在");
        }
        return Result.success(handbook);
    }

    /**
     * 管理端 - 新增手册
     */
    @PostMapping("/admin/add")
    public Result<?> addHandbook(@RequestBody Handbook handbook) {
        try {
            // 设置默认值

            if (handbook.getViewCount() == null) {
                handbook.setViewCount(0);
            }
            if (handbook.getSortOrder() == null) {
                handbook.setSortOrder(0);
            }
            
            handbookMapper.insert(handbook);
            return Result.success(handbook);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 管理端 - 更新手册
     */
    @PutMapping("/admin/update")
    public Result<?> updateHandbook(@RequestBody Handbook handbook) {
        try {
            if (handbook.getId() == null) {
                return Result.error("手册ID不能为空");
            }
            
            Handbook existHandbook = handbookMapper.selectById(handbook.getId());
            if (existHandbook == null) {
                return Result.error("手册不存在");
            }
            
            handbookMapper.updateById(handbook);
            return Result.success(handbook);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 管理端 - 删除手册
     */
    @DeleteMapping("/admin/delete/{id}")
    public Result<?> deleteHandbook(@PathVariable Integer id) {
        try {
            Handbook handbook = handbookMapper.selectById(id);
            if (handbook == null) {
                return Result.error("手册不存在");
            }
            
            handbookMapper.deleteById(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 管理端 - 批量删除
     */
    @DeleteMapping("/admin/batch-delete")
    public Result<?> batchDelete(@RequestBody List<Integer> ids) {
        try {
            handbookMapper.deleteBatchIds(ids);
            return Result.success("批量删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }

    /**
     * 管理端 - 切换发布状态
     */
    @PutMapping("/admin/toggle-publish/{id}")
    public Result<?> togglePublish(@PathVariable Integer id) {
        try {
            Handbook handbook = handbookMapper.selectById(id);
            if (handbook == null) {
                return Result.error("手册不存在");
            }
            
            handbook.setIsPublished(handbook.getIsPublished()==0?0:1);
            handbookMapper.updateById(handbook);
            return Result.success(handbook);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 管理端 - 更新排序
     */
    @PutMapping("/admin/update-sort/{id}")
    public Result<?> updateSort(@PathVariable Integer id, @RequestParam Integer sortOrder) {
        try {
            Handbook handbook = handbookMapper.selectById(id);
            if (handbook == null) {
                return Result.error("手册不存在");
            }
            
            handbook.setSortOrder(sortOrder);
            handbookMapper.updateById(handbook);
            return Result.success(handbook);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }
}

