package com.fishseedling.platform.controller;

import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.entity.Report;
import com.fishseedling.platform.service.ReportService;
import com.fishseedling.platform.util.IpUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 举报Controller（前台）
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/front/report")
public class ReportController {

    @Resource
    private ReportService reportService;

    /**
     * 提交举报（支持信息和物流）
     */
    @PostMapping("/submit")
    public Result<Void> submitReport(@RequestBody Report report,
                                     HttpServletRequest request) {
        try {
            // 验证必填字段
            if (report.getTargetId() == null) {
                return Result.error("被举报对象ID不能为空");
            }
            if (report.getTargetType() == null || report.getTargetType().isEmpty()) {
                return Result.error("被举报对象类型不能为空");
            }
            if (report.getReason() == null || report.getReason().isEmpty()) {
                return Result.error("请选择举报原因");
            }
            if (report.getDescription() == null || report.getDescription().isEmpty()) {
                return Result.error("请填写举报描述");
            }
            
            // 获取举报人IP
            String ip = IpUtil.getIpAddress(request);
            
            boolean success = reportService.submitReport(
                    report.getTargetType(),
                    report.getTargetId(),
                    report.getReason(),
                    report.getDescription(),
                    report.getReporterContact(),
                    ip
            );
            
            if (success) {
                return Result.success("举报提交成功，我们会尽快处理");
            } else {
                return Result.error("您已经举报过该信息，请勿重复举报");
            }
        } catch (Exception e) {
            return Result.error("举报失败：" + e.getMessage());
        }
    }
}

