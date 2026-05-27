package com.fishseedling.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.entity.Admin;
import com.fishseedling.platform.entity.AdminOperationLog;
import com.fishseedling.platform.mapper.AdminMapper;
import com.fishseedling.platform.mapper.AdminOperationLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员操作日志Service
 *
 * @author Fish Seedling Platform
 * @since 2025-10-14
 */
@Service
public class AdminOperationLogService {

    @Resource
    private AdminOperationLogMapper adminOperationLogMapper;

    @Resource
    private AdminMapper adminMapper;

    /**
     * 分页查询操作日志
     */
    public PageResult<Map<String, Object>> getLogsByPage(Integer current, Integer size,
                                                          Integer adminId, String operationType,
                                                          LocalDate startDate, LocalDate endDate) {
        Page<AdminOperationLog> page = new Page<>(current, size);
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();

        // 按管理员ID筛选
        if (adminId != null) {
            wrapper.eq(AdminOperationLog::getAdminId, adminId);
        }

        // 按操作类型筛选
        if (operationType != null && !operationType.trim().isEmpty()) {
            wrapper.eq(AdminOperationLog::getOperationType, operationType);
        }

        // 按日期范围筛选
        if (startDate != null) {
            LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
            wrapper.ge(AdminOperationLog::getCreateTime, startDateTime);
        }
        if (endDate != null) {
            LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);
            wrapper.le(AdminOperationLog::getCreateTime, endDateTime);
        }

        // 按时间倒序排列
        wrapper.orderByDesc(AdminOperationLog::getCreateTime);

        IPage<AdminOperationLog> result = adminOperationLogMapper.selectPage(page, wrapper);

        // 查询所有管理员信息，用于显示管理员用户名
        List<Admin> admins = adminMapper.selectList(null);
        Map<Integer, String> adminMap = admins.stream()
                .collect(Collectors.toMap(Admin::getId, Admin::getUsername));

        // 转换为Map，添加管理员用户名
        List<Map<String, Object>> records = result.getRecords().stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("adminId", log.getAdminId());
            map.put("adminUsername", adminMap.getOrDefault(log.getAdminId(), "未知"));
            map.put("operationType", log.getOperationType());
            map.put("operationObject", log.getOperationObject());
            map.put("operationDetail", log.getOperationDetail());
            map.put("ipAddress", log.getIpAddress());
            map.put("createTime", log.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        PageResult<Map<String, Object>> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(result.getTotal());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setPages(result.getPages());

        return pageResult;
    }

    /**
     * 记录操作日志
     */
    public void log(Integer adminId, String operationType, String operationObject,
                    String operationDetail, String ipAddress) {
        // 防护性检查：adminId不能为null
        if (adminId == null) {
            System.err.println("警告：尝试记录操作日志但adminId为null，操作类型：" + operationType);
            return;
        }
        
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setOperationType(operationType);
        log.setOperationObject(operationObject);
        log.setOperationDetail(operationDetail);
        log.setIpAddress(ipAddress);
        adminOperationLogMapper.insert(log);
    }

    /**
     * 获取操作类型统计
     */
    public Map<String, Long> getOperationTypeStats(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (startDate != null) {
            LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
            wrapper.ge(AdminOperationLog::getCreateTime, startDateTime);
        }
        if (endDate != null) {
            LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);
            wrapper.le(AdminOperationLog::getCreateTime, endDateTime);
        }

        List<AdminOperationLog> logs = adminOperationLogMapper.selectList(wrapper);

        return logs.stream()
                .collect(Collectors.groupingBy(
                        AdminOperationLog::getOperationType,
                        Collectors.counting()
                ));
    }

    /**
     * 获取管理员操作统计
     */
    public List<Map<String, Object>> getAdminOperationStats(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (startDate != null) {
            LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
            wrapper.ge(AdminOperationLog::getCreateTime, startDateTime);
        }
        if (endDate != null) {
            LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);
            wrapper.le(AdminOperationLog::getCreateTime, endDateTime);
        }

        List<AdminOperationLog> logs = adminOperationLogMapper.selectList(wrapper);

        // 查询所有管理员
        List<Admin> admins = adminMapper.selectList(null);
        Map<Integer, String> adminMap = admins.stream()
                .collect(Collectors.toMap(Admin::getId, Admin::getUsername));

        // 按管理员ID分组统计
        Map<Integer, Long> countMap = logs.stream()
                .collect(Collectors.groupingBy(
                        AdminOperationLog::getAdminId,
                        Collectors.counting()
                ));

        // 转换为List
        return countMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("adminId", entry.getKey());
                    map.put("adminUsername", adminMap.getOrDefault(entry.getKey(), "未知"));
                    map.put("count", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }
}

