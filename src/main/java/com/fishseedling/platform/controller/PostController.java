package com.fishseedling.platform.controller;

import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.dto.PostPublishDTO;
import com.fishseedling.platform.dto.PostQueryDTO;
import com.fishseedling.platform.entity.Post;
import com.fishseedling.platform.service.PostService;
import com.fishseedling.platform.util.IpUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 信息发布Controller（前台）
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/front/post")
public class PostController {

    @Resource
    private PostService postService;

    /**
     * 发布信息
     */
    @PostMapping("/publish")
    public Result<Map<String, Object>> publishPost(@Validated @RequestBody PostPublishDTO dto,
                                                     HttpServletRequest request) {
        try {
            String ip = IpUtil.getIpAddress(request);
            String device = request.getHeader("User-Agent");
            Map<String, Object> result = postService.publishPost(dto, ip, device);
            return Result.success("发布成功！您的信息已提交，通过审核后将在15分钟内上线展示。", result);
        } catch (Exception e) {
            return Result.error("发布失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询信息列表
     */
    @PostMapping("/list")
    public Result<PageResult<Post>> queryList(@RequestBody PostQueryDTO queryDTO) {
        try {
            // 前台只查询已审核通过的信息
            queryDTO.setStatus("approved");
            PageResult<Post> result = postService.queryPostsByPage(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取信息详情
     */
    @GetMapping("/detail/{id}")
    public Result<Post> getDetail(@PathVariable Long id) {
        try {
            Post post = postService.getPostDetail(id);
            if (post == null) {
                return Result.error("信息不存在");
            }
            return Result.success(post);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 查看完整电话号码
     */
    @GetMapping("/phone/{id}")
    public Result<String> getFullPhone(@PathVariable Long id) {
        try {
            String phone = postService.getFullPhone(id);
            if (phone == null) {
                return Result.error("信息不存在");
            }
            return Result.success(phone);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 通过管理码查询信息
     */
    @GetMapping("/manage/{manageCode}")
    public Result<Post> getByManageCode(@PathVariable String manageCode) {
        try {
            Post post = postService.getPostByManageCode(manageCode);
            if (post == null) {
                return Result.error("信息不存在或管理码已过期");
            }
            return Result.success(post);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 更新信息
     */
    @PutMapping("/update/{id}")
    public Result<Void> updatePost(@PathVariable Long id,
                                    @Validated @RequestBody PostPublishDTO dto,
                                    @RequestHeader(value = "X-Manage-Code", required = false) String manageCode) {
        try {
            // 如果提供了管理码，则进行验证
            if (manageCode != null && !manageCode.isEmpty()) {
                boolean isValid = postService.validateManageCode(id, manageCode);
                if (!isValid) {
                    return Result.error("管理码无效或已过期");
                }
            }
            
            boolean success = postService.updatePost(id, dto);
            if (success) {
                Result<Void> result = Result.success("修改成功，信息将重新进入审核");
                return result;
            } else {
                return Result.error("修改失败");
            }
        } catch (Exception e) {
            return Result.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 删除信息
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        try {
            boolean success = postService.deletePost(id);
            if (success) {
                Result<Void> result = Result.success("删除成功");
                return result;
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取首页数据
     */
    @GetMapping("/home")
    public Result<Map<String, Object>> getHomePageData() {
        try {
            Map<String, Object> result = postService.getHomePageData();
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}

