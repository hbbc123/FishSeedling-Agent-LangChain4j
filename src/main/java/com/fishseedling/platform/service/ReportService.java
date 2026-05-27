package com.fishseedling.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.entity.Report;
import com.fishseedling.platform.mapper.ReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 举报服务类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Service
public class ReportService {

    @Resource
    private ReportMapper reportMapper;

    /**
     * 提交举报（支持信息和物流）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean submitReport(String targetType, Long targetId, String reason, String description, 
                                 String reporterContact, String reporterIp) {
        // 检查是否重复举报（同一IP在1小时内对同一对象的举报）
        LambdaQueryWrapper<Report> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Report::getTargetType, targetType)
                   .eq(Report::getTargetId, targetId)
                   .eq(Report::getReporterIp, reporterIp)
                   .ge(Report::getCreateTime, LocalDateTime.now().minusHours(1));
        
        Long count = reportMapper.selectCount(checkWrapper);
        if (count > 0) {
            // 重复举报
            return false;
        }

        Report report = new Report();
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setDescription(description);
        report.setReporterContact(reporterContact);
        report.setReporterIp(reporterIp);
        report.setStatus("pending");

        return reportMapper.insert(report) > 0;
    }

    /**
     * 分页查询举报列表（支持多种筛选条件）
     */
    public PageResult<Report> getReportsByPage(Integer current, Integer size, String status, 
                                               String targetType, Long targetId) {
        Page<Report> page = new Page<>(current, size);
        
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        
        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(Report::getStatus, status);
        }
        
        // 对象类型筛选
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(Report::getTargetType, targetType);
        }
        
        // 对象ID筛选
        if (targetId != null) {
            wrapper.eq(Report::getTargetId, targetId);
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Report::getCreateTime);
        
        IPage<Report> result = reportMapper.selectPage(page, wrapper);
        
        return new PageResult<>(result.getRecords(), result.getTotal(), 
                               result.getCurrent(), result.getSize());
    }

    /**
     * 获取举报详情
     */
    public Report getReportById(Long id) {
        return reportMapper.selectById(id);
    }

    /**
     * 处理举报
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean processReport(Long id, String status, String processResult, Integer processorId) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            return false;
        }

        report.setStatus(status);
        report.setProcessResult(processResult);
        report.setProcessorId(processorId);
        report.setProcessTime(LocalDateTime.now());

        return reportMapper.updateById(report) > 0;
    }

    /**
     * 删除举报
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReport(Long id) {
        return reportMapper.deleteById(id) > 0;
    }

    /**
     * 批量处理举报
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchProcessReports(List<Long> ids, String status, String processResult, Integer processorId) {
        int count = 0;
        for (Long id : ids) {
            if (processReport(id, status, processResult, processorId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计待处理举报数量
     */
    public Long countPendingReports() {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getStatus, "pending");
        return reportMapper.selectCount(wrapper);
    }

    /**
     * 根据目标对象查询举报列表
     */
    public List<Report> getReportsByTarget(String targetType, Long targetId) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getTargetType, targetType)
               .eq(Report::getTargetId, targetId)
               .orderByDesc(Report::getCreateTime);
        return reportMapper.selectList(wrapper);
    }

    /**
     * 统计各类型待处理举报数量
     */
    public Map<String, Object> countPendingByType() {
        Map<String, Object> result = new HashMap<>();
        
        // 统计信息类型的待处理数量
        LambdaQueryWrapper<Report> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(Report::getStatus, "pending")
                   .eq(Report::getTargetType, "post");
        Long postPendingCount = reportMapper.selectCount(postWrapper);
        
        // 统计物流类型的待处理数量
        LambdaQueryWrapper<Report> logisticsWrapper = new LambdaQueryWrapper<>();
        logisticsWrapper.eq(Report::getStatus, "pending")
                       .eq(Report::getTargetType, "logistics");
        Long logisticsPendingCount = reportMapper.selectCount(logisticsWrapper);
        
        // 统计总待处理数量
        Long totalPendingCount = postPendingCount + logisticsPendingCount;
        
        result.put("postPendingCount", postPendingCount);
        result.put("logisticsPendingCount", logisticsPendingCount);
        result.put("totalPendingCount", totalPendingCount);
        
        // 统计已处理数量
        LambdaQueryWrapper<Report> processedWrapper = new LambdaQueryWrapper<>();
        processedWrapper.eq(Report::getStatus, "processed");
        Long processedCount = reportMapper.selectCount(processedWrapper);
        result.put("processedCount", processedCount);
        
        // 统计已忽略数量
        LambdaQueryWrapper<Report> ignoredWrapper = new LambdaQueryWrapper<>();
        ignoredWrapper.eq(Report::getStatus, "ignored");
        Long ignoredCount = reportMapper.selectCount(ignoredWrapper);
        result.put("ignoredCount", ignoredCount);
        
        // 统计总数
        Long totalCount = reportMapper.selectCount(null);
        result.put("totalCount", totalCount);
        
        return result;
    }
}

