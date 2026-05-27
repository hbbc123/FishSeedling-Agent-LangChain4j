package com.fishseedling.platform.controller;

import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.entity.Admin;
import com.fishseedling.platform.service.AdminService;
import com.fishseedling.platform.service.AdminOperationLogService;
import com.fishseedling.platform.util.JwtUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理员管理Controller（后台）
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/admin/manage")
public class AdminManageController {

    @Resource
    private AdminService adminService;

    @Resource
    private AdminOperationLogService adminOperationLogService;

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 分页查询管理员列表
     */
    @GetMapping("/list")
    public Result<PageResult<Admin>> getAdminList(@RequestParam(defaultValue = "1") Integer current,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String role) {
        try {
            PageResult<Admin> result = adminService.getAdminsByPage(current, size, keyword, status, role);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有管理员列表（不分页）
     */
    @GetMapping("/all")
    public Result<List<Admin>> getAllAdmins() {
        try {
            List<Admin> admins = adminService.getAllAdmins();
            return Result.success(admins);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取管理员详情
     */
    @GetMapping("/detail/{id}")
    public Result<Admin> getAdminDetail(@PathVariable Integer id) {
        try {
            Admin admin = adminService.getAdminById(id);
            if (admin == null) {
                return Result.error("管理员不存在");
            }
            // 不返回密码
            admin.setPassword(null);
            return Result.success(admin);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 创建管理员
     */
    @PostMapping("/create")
    public Result<Void> createAdmin(@RequestBody Map<String, String> params,
                                    HttpServletRequest request) {
        try {
            String username = params.get("username");
            String password = params.get("password");
            String role = params.get("role");

            // 验证必填字段
            if (username == null || username.trim().isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return Result.error("密码不能为空");
            }
            if (password.length() < 6) {
                return Result.error("密码长度不能少于6位");
            }
            if (role == null || role.trim().isEmpty()) {
                return Result.error("请选择角色");
            }

            //todo 验证权限（只有超级管理员可以创建管理员）
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer currentAdminId = jwtUtil.getAdminIdFromToken(token);
            Admin currentAdmin = adminService.getAdminById(currentAdminId);

            if (currentAdmin == null || !"super".equals(currentAdmin.getRole())) {
                return Result.error("权限不足，只有超级管理员可以创建管理员");
            }

            boolean success = adminService.createAdmin(username, password, role);
            if (success) {
                //todo 记录创建管理员日志
                String ip = com.fishseedling.platform.util.IpUtil.getIpAddress(request);
                adminOperationLogService.log(
                        currentAdminId,
                        "创建管理员",
                        "管理员: " + username,
                        "创建了新管理员 " + username + "，角色：" + role,
                        ip
                );
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新管理员信息
     */
    @PutMapping("/update/{id}")
    public Result<Void> updateAdmin(@PathVariable Integer id,
                                    @RequestBody Map<String, String> params,
                                    HttpServletRequest request) {
        try {
            String username = params.get("username");
            String role = params.get("role");

            // 验证权限
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer currentAdminId = jwtUtil.getAdminIdFromToken(token);
            Admin currentAdmin = adminService.getAdminById(currentAdminId);

            if (currentAdmin == null || !"super".equals(currentAdmin.getRole())) {
                return Result.error("权限不足，只有超级管理员可以修改管理员信息");
            }

            boolean success = adminService.updateAdmin(id, username, role);
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
     * 删除管理员
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteAdmin(@PathVariable Integer id,
                                    HttpServletRequest request) {
        try {
            // 验证权限
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer currentAdminId = jwtUtil.getAdminIdFromToken(token);
            Admin currentAdmin = adminService.getAdminById(currentAdminId);

            if (currentAdmin == null || !"super".equals(currentAdmin.getRole())) {
                return Result.error("权限不足，只有超级管理员可以删除管理员");
            }

            // 不能删除自己
            if (id.equals(currentAdminId)) {
                return Result.error("不能删除自己");
            }

            // 先获取要删除的管理员信息
            Admin targetAdmin = adminService.getAdminById(id);
            
            boolean success = adminService.deleteAdmin(id);
            if (success) {
                // 记录删除管理员日志
                String ip = com.fishseedling.platform.util.IpUtil.getIpAddress(request);
                adminOperationLogService.log(
                        currentAdminId,
                        "删除管理员",
                        "管理员ID: " + id,
                        "删除了管理员 " + (targetAdmin != null ? targetAdmin.getUsername() : id),
                        ip
                );
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 重置密码
     */
    @PutMapping("/reset-password/{id}")
    public Result<Void> resetPassword(@PathVariable Integer id,
                                      @RequestBody Map<String, String> params,
                                      HttpServletRequest request) {
        try {
            String newPassword = params.get("newPassword");

            if (newPassword == null || newPassword.trim().isEmpty()) {
                return Result.error("新密码不能为空");
            }
            if (newPassword.length() < 6) {
                return Result.error("密码长度不能少于6位");
            }

            // 验证权限
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer currentAdminId = jwtUtil.getAdminIdFromToken(token);
            Admin currentAdmin = adminService.getAdminById(currentAdminId);

            if (currentAdmin == null || !"super".equals(currentAdmin.getRole())) {
                return Result.error("权限不足，只有超级管理员可以重置密码");
            }

            boolean success = adminService.resetPassword(id, newPassword);
            if (success) {
                return Result.success("密码重置成功");
            } else {
                return Result.error("密码重置失败");
            }
        } catch (Exception e) {
            return Result.error("密码重置失败：" + e.getMessage());
        }
    }

    /**
     * 修改自己的密码
     */
    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params,
                                       HttpServletRequest request) {
        try {
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");

            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                return Result.error("原密码不能为空");
            }
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return Result.error("新密码不能为空");
            }
            if (newPassword.length() < 6) {
                return Result.error("新密码长度不能少于6位");
            }

            // 获取当前登录管理员
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer currentAdminId = jwtUtil.getAdminIdFromToken(token);

            boolean success = adminService.changePassword(currentAdminId, oldPassword, newPassword);
            if (success) {
                // 记录修改密码日志
                String ip = com.fishseedling.platform.util.IpUtil.getIpAddress(request);
                adminOperationLogService.log(
                        currentAdminId,
                        "修改密码",
                        "修改自己的密码",
                        "管理员修改了自己的登录密码",
                        ip
                );
                return Result.success("密码修改成功");
            } else {
                return Result.error("密码修改失败");
            }
        } catch (Exception e) {
            return Result.error("密码修改失败：" + e.getMessage());
        }
    }

    /**
     * 锁定/解锁账号
     */
    @PutMapping("/toggle-lock/{id}")
    public Result<Void> toggleLock(@PathVariable Integer id,
                                   HttpServletRequest request) {
        try {
            // 验证权限
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer currentAdminId = jwtUtil.getAdminIdFromToken(token);
            Admin currentAdmin = adminService.getAdminById(currentAdminId);

            if (currentAdmin == null || !"super".equals(currentAdmin.getRole())) {
                return Result.error("权限不足，只有超级管理员可以锁定/解锁账号");
            }

            // 不能锁定自己
            if (id.equals(currentAdminId)) {
                return Result.error("不能锁定自己");
            }

            boolean success = adminService.toggleLock(id);
            if (success) {
                return Result.success("操作成功");
            } else {
                return Result.error("操作失败");
            }
        } catch (Exception e) {
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询操作日志
     */
    @GetMapping("/logs")
    public Result<PageResult<Map<String, Object>>> getOperationLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer adminId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            PageResult<Map<String, Object>> result = adminOperationLogService.getLogsByPage(
                    current, size, adminId, operationType, startDate, endDate);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取操作类型统计
     */
    @GetMapping("/logs/type-stats")
    public Result<Map<String, Long>> getOperationTypeStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            Map<String, Long> stats = adminOperationLogService.getOperationTypeStats(startDate, endDate);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取管理员操作统计
     */
    @GetMapping("/logs/admin-stats")
    public Result<List<Map<String, Object>>> getAdminOperationStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            List<Map<String, Object>> stats = adminOperationLogService.getAdminOperationStats(startDate, endDate);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}

