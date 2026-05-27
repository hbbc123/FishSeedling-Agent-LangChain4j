package com.fishseedling.platform.controller;

import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.entity.HelpContent;
import com.fishseedling.platform.service.HelpContentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 帮助内容 Controller
 * 管理端 CRUD + 前台查询
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
@RestController
@RequestMapping("/help-content")
public class HelpContentController {

    @Resource
    private HelpContentService helpContentService;

    // ==================== 前台接口 ====================

    /**
     * 获取所有启用的帮助内容（按分类分组）
     */
    @GetMapping("/public/grouped")
    public Result<Map<String, List<HelpContent>>> getGroupedByCategory() {
        try {
            Map<String, List<HelpContent>> result = helpContentService.getGroupedByCategory();
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有启用的帮助内容（列表）
     */
    @GetMapping("/public/list")
    public Result<List<HelpContent>> getAllEnabled() {
        try {
            List<HelpContent> result = helpContentService.getAllEnabled();
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 按分类获取启用的帮助内容
     */
    @GetMapping("/public/category/{category}")
    public Result<List<HelpContent>> getByCategory(@PathVariable String category) {
        try {
            List<HelpContent> result = helpContentService.getByCategoryEnabled(category);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 管理端接口 ====================

    /**
     * 分页查询（管理端）
     */
    @GetMapping("/admin/list")
    public Result<PageResult<HelpContent>> getAdminList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        try {
            PageResult<HelpContent> result = helpContentService.getHelpContentByPage(current, size, category, keyword);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取详情
     */
    @GetMapping("/admin/detail/{id}")
    public Result<HelpContent> getDetail(@PathVariable Long id) {
        try {
            HelpContent helpContent = helpContentService.getById(id);
            if (helpContent == null) {
                return Result.error("记录不存在");
            }
            return Result.success(helpContent);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 新增
     */
    @PostMapping("/admin/create")
    public Result<Void> create(@RequestBody HelpContent helpContent) {
        try {
            boolean success = helpContentService.create(helpContent);
            if (success) {
                return Result.success("新增成功");
            } else {
                return Result.error("新增失败");
            }
        } catch (Exception e) {
            return Result.error("新增失败：" + e.getMessage());
        }
    }

    /**
     * 更新
     */
    @PutMapping("/admin/update")
    public Result<Void> update(@RequestBody HelpContent helpContent) {
        try {
            if (helpContent.getId() == null) {
                return Result.error("ID不能为空");
            }
            boolean success = helpContentService.update(helpContent);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除
     */
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            boolean success = helpContentService.delete(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }
}
