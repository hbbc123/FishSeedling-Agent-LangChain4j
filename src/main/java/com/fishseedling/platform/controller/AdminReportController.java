package com.fishseedling.platform.controller;

import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.entity.Report;
import com.fishseedling.platform.service.ReportService;
import com.fishseedling.platform.service.AdminOperationLogService;
import com.fishseedling.platform.util.IpUtil;
import com.fishseedling.platform.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 举报管理Controller（后台）
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/admin/report")
public class AdminReportController {

    @Resource
    private ReportService reportService;

    @Resource
    private AdminOperationLogService adminOperationLogService;

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 分页查询举报列表（支持多种筛选条件）
     */
    @GetMapping("/list")
    public Result<PageResult<Report>> getReportList(@RequestParam(defaultValue = "1") Integer current,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String targetType,
                                                     @RequestParam(required = false) Long targetId) {
        try {
            PageResult<Report> result = reportService.getReportsByPage(current, size, status, targetType, targetId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取举报详情
     */
    @GetMapping("/detail/{id}")
    public Result<Report> getReportDetail(@PathVariable Long id) {
        try {
            Report report = reportService.getReportById(id);
            if (report == null) {
                return Result.error("举报记录不存在");
            }
            return Result.success(report);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 处理举报
     */
    @PutMapping("/process/{id}")
    public Result<Void> processReport(@PathVariable Long id,
                                      @RequestBody Map<String, String> params,
                                      HttpServletRequest request) {
        try {
            String status = params.get("status");
            String processResult = params.get("processResult");
            
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer adminId = jwtUtil.getAdminIdFromToken(token);
            
            if (adminId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            // 先获取举报详情
            Report report = reportService.getReportById(id);
            
            boolean success = reportService.processReport(id, status, processResult, adminId);
            
            if (success) {
                // 记录处理举报日志
                String ip = IpUtil.getIpAddress(request);
                String detail = "处理了ID为" + id + "的举报";
                if (report != null) {
                    detail += "，举报类型：" + report.getTargetType() + "，处理结果：" + status;
                }
                adminOperationLogService.log(
                        adminId,
                        "处理举报",
                        "举报ID: " + id,
                        detail,
                        ip
                );
                return Result.success("处理成功");
            } else {
                return Result.error("处理失败");
            }
        } catch (Exception e) {
            return Result.error("处理失败：" + e.getMessage());
        }
    }

    /**
     * 批量处理举报
     */
    @PutMapping("/batch-process")
    public Result<String> batchProcessReports(@RequestBody Map<String, Object> params,
                                              HttpServletRequest request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) params.get("ids");
            String status = (String) params.get("status");
            String processResult = (String) params.get("processResult");
            
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer adminId = jwtUtil.getAdminIdFromToken(token);
            
            if (adminId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            int count = reportService.batchProcessReports(ids, status, processResult, adminId);
            
            return Result.success("成功处理 " + count + " 条举报");
        } catch (Exception e) {
            return Result.error("批量处理失败：" + e.getMessage());
        }
    }

    /**
     * 删除举报
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteReport(@PathVariable Long id) {
        try {
            boolean success = reportService.deleteReport(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 统计待处理举报数量
     */
    @GetMapping("/pending-count")
    public Result<Long> getPendingCount() {
        try {
            Long count = reportService.countPendingReports();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 统计各类型待处理举报数量
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getReportStats() {
        try {
            Map<String, Object> stats = reportService.countPendingByType();
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据目标对象查询举报列表
     */
    @GetMapping("/by-target")
    public Result<List<Report>> getReportsByTarget(@RequestParam String targetType,
                                                   @RequestParam Long targetId) {
        try {
            List<Report> reports = reportService.getReportsByTarget(targetType, targetId);
            return Result.success(reports);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}

