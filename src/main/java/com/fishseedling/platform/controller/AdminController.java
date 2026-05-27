package com.fishseedling.platform.controller;

import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.entity.Admin;
import com.fishseedling.platform.mapper.LogisticsMapper;
import com.fishseedling.platform.mapper.PostMapper;
import com.fishseedling.platform.service.AdminService;
import com.fishseedling.platform.service.AdminOperationLogService;
import com.fishseedling.platform.util.IpUtil;
import com.fishseedling.platform.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理员Controller
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @Resource
    private AdminOperationLogService adminOperationLogService;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private PostMapper postMapper;

    @Resource
    private LogisticsMapper logisticsMapper;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String username,
                                              @RequestParam String password,
                                              HttpServletRequest request) {
        try {
            String ip = IpUtil.getIpAddress(request);
            Map<String, Object> result = adminService.login(username, password, ip);
            
            // 记录登录日志
            Admin admin = (Admin) result.get("admin");
            if (admin != null && admin.getId() != null) {
                adminOperationLogService.log(
                        admin.getId(),
                        "登录",
                        "后台管理系统",
                        "管理员 " + username + " 登录成功",
                        ip
                );
            }
            
            return Result.success("登录成功", result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取每日信息发布数量统计（最近30天）
     */
    @GetMapping("/stats/daily-posts")
    public Result<Map<String, Object>> getDailyPostStats() {
        try {
            // 获取数据库统计数据
            List<Map<String, Object>> dbData = postMapper.selectDailyPostCount();
            
            // 创建日期映射
            Map<String, Long> dataMap = new HashMap<>();
            for (Map<String, Object> item : dbData) {
                String date = item.get("date").toString();
                Long count = ((Number) item.get("count")).longValue();
                dataMap.put(date, count);
            }
            
            // 生成最近30天的完整日期列表
            List<String> dates = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (int i = 29; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                String dateStr = date.format(formatter);
                dates.add(dateStr);
                counts.add(dataMap.getOrDefault(dateStr, 0L));
            }
            
            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates);
            result.put("counts", counts);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取每日物流信息发布数量统计（最近30天）
     */
    @GetMapping("/stats/daily-logistics")
    public Result<Map<String, Object>> getDailyLogisticsStats() {
        try {
            // 获取数据库统计数据
            List<Map<String, Object>> dbData = logisticsMapper.selectDailyLogisticsCount();
            
            // 创建日期映射
            Map<String, Long> dataMap = new HashMap<>();
            for (Map<String, Object> item : dbData) {
                String date = item.get("date").toString();
                Long count = ((Number) item.get("count")).longValue();
                dataMap.put(date, count);
            }
            
            // 生成最近30天的完整日期列表
            List<String> dates = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (int i = 29; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                String dateStr = date.format(formatter);
                dates.add(dateStr);
                counts.add(dataMap.getOrDefault(dateStr, 0L));
            }
            
            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates);
            result.put("counts", counts);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取物流统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取综合统计数据
     */
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> getOverviewStats() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 信息统计
            Long totalPosts = postMapper.selectCount(null);
            Long pendingPosts = postMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.fishseedling.platform.entity.Post>()
                    .eq(com.fishseedling.platform.entity.Post::getStatus, "pending")
            );
            Long todayPosts = postMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.fishseedling.platform.entity.Post>()
                    .ge(com.fishseedling.platform.entity.Post::getCreateTime, LocalDate.now().atStartOfDay())
            );
            
            // 物流统计
            Long totalLogistics = logisticsMapper.selectCount(null);
            Long todayLogistics = logisticsMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.fishseedling.platform.entity.Logistics>()
                    .ge(com.fishseedling.platform.entity.Logistics::getCreateTime, LocalDate.now().atStartOfDay())
            );
            
            result.put("totalPosts", totalPosts);
            result.put("pendingPosts", pendingPosts);
            result.put("todayPosts", todayPosts);
            result.put("totalLogistics", totalLogistics);
            result.put("todayLogistics", todayLogistics);
            result.put("totalAll", totalPosts + totalLogistics);
            result.put("todayAll", todayPosts + todayLogistics);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取综合统计数据失败：" + e.getMessage());
        }
    }
}

