package com.fishseedling.platform.controller;

import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.dto.PostQueryDTO;
import com.fishseedling.platform.entity.Post;
import com.fishseedling.platform.service.PostService;
import com.fishseedling.platform.service.AdminOperationLogService;
import com.fishseedling.platform.util.IpUtil;
import com.fishseedling.platform.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 信息管理Controller（后台）
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/admin/post")
public class AdminPostController {

    @Resource
    private PostService postService;

    @Resource
    private AdminOperationLogService adminOperationLogService;

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 分页查询信息列表（后台）
     * 管理端显示完整的解密后的联系电话
     */
    @PostMapping("/list")
    public Result<PageResult<Post>> queryList(@RequestBody PostQueryDTO queryDTO) {
        try {
            // 管理端查询，电话不脱敏，显示完整号码
            PageResult<Post> result = postService.queryPostsByPage(queryDTO, false);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 审核信息
     */
    @PostMapping("/audit")
    public Result<Void> auditPost(@RequestParam Long id,
                                   @RequestParam String status,
                                   @RequestParam(required = false) String rejectReason,
                                   @RequestParam Integer auditorId,
                                   HttpServletRequest request) {
        try {
            Post post = postService.getPostDetail(id);
            boolean success = postService.auditPost(id, status, rejectReason, auditorId);
            if (success) {
                // 记录审核日志
                String ip = IpUtil.getIpAddress(request);
                String statusText = "approved".equals(status) ? "通过" : "拒绝";
                String detail = "审核" + statusText + "了ID为" + id + "的" + 
                        (post != null ? post.getType() : "") + "信息";
                if (rejectReason != null&&rejectReason.length() > 0) {
                    detail += "，拒绝原因：" + rejectReason;
                }
                adminOperationLogService.log(
                        auditorId,
                        "审核信息",
                        "信息ID: " + id,
                        detail,
                        ip
                );
                
                Result<Void> result = Result.success("审核成功");
                return result;
            } else {
                return Result.error("审核失败");
            }
        } catch (Exception e) {
            return Result.error("审核失败：" + e.getMessage());
        }
    }

    /**
     * 置顶信息
     */
    @PostMapping("/top")
    public Result<Void> topPost(@RequestParam Long id,
                                 @RequestParam(defaultValue = "0") Integer days,
                                 HttpServletRequest request) {
        try {
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer adminId = jwtUtil.getAdminIdFromToken(token);
            
            if (adminId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            // 先获取信息详情用于日志记录
            Post post = postService.getPostDetail(id);
            
            boolean success = postService.topPost(id, days);
            if (success) {
                // 记录置顶日志
                String ip = IpUtil.getIpAddress(request);
                String detail = "置顶了ID为" + id + "的" + 
                        (post != null ? post.getType() : "") + "信息，置顶天数：" + days + "天";
                if (post != null && post.getFishBreed() != null) {
                    detail += "，品种：" + post.getFishBreed();
                }
                adminOperationLogService.log(
                        adminId,
                        "置顶信息",
                        "信息ID: " + id,
                        detail,
                        ip
                );
                
                Result<Void> result = Result.success("置顶成功");
                return result;
            } else {
                return Result.error("置顶失败");
            }
        } catch (Exception e) {
            return Result.error("置顶失败：" + e.getMessage());
        }
    }

    /**
     * 取消置顶
     */
    @PostMapping("/cancel-top")
    public Result<Void> cancelTop(@RequestParam Long id,
                                   HttpServletRequest request) {
        try {
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer adminId = jwtUtil.getAdminIdFromToken(token);
            
            if (adminId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            // 先获取信息详情用于日志记录
            Post post = postService.getPostDetail(id);
            
            boolean success = postService.cancelTop(id);
            if (success) {
                // 记录取消置顶日志
                String ip = IpUtil.getIpAddress(request);
                String detail = "取消置顶了ID为" + id + "的" + 
                        (post != null ? post.getType() : "") + "信息";
                if (post != null && post.getFishBreed() != null) {
                    detail += "，品种：" + post.getFishBreed();
                }
                adminOperationLogService.log(
                        adminId,
                        "取消置顶信息",
                        "信息ID: " + id,
                        detail,
                        ip
                );
                
                Result<Void> result = Result.success("取消置顶成功");
                return result;
            } else {
                return Result.error("取消置顶失败");
            }
        } catch (Exception e) {
            return Result.error("取消置顶失败：" + e.getMessage());
        }
    }

    /**
     * 删除信息
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePost(@PathVariable Long id,
                                    HttpServletRequest request) {
        try {
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer adminId = jwtUtil.getAdminIdFromToken(token);
            
            if (adminId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            // 先获取信息详情用于日志记录
            Post post = postService.getPostDetail(id);
            
            boolean success = postService.deletePost(id);
            if (success) {
                // 记录删除日志
                String ip = IpUtil.getIpAddress(request);
                String detail = "删除了ID为" + id + "的" + 
                        (post != null ? post.getType() : "") + "信息";
                if (post != null && post.getFishBreed() != null) {
                    detail += "，品种：" + post.getFishBreed();
                }
                adminOperationLogService.log(
                        adminId,
                        "删除信息",
                        "信息ID: " + id,
                        detail,
                        ip
                );
                
                Result<Void> result = Result.success("删除成功");
                return result;
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }
}

