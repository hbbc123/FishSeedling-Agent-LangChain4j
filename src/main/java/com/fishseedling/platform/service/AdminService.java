package com.fishseedling.platform.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.entity.Admin;
import com.fishseedling.platform.mapper.AdminMapper;
import com.fishseedling.platform.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员服务类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Service
public class AdminService {

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 管理员登录
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> login(String username, String password, String ip) {
        // 查询管理员
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        Admin admin = adminMapper.selectOne(wrapper);

        if (admin == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查账号状态
        if ("locked".equals(admin.getStatus())) {
            if (admin.getLockUntil() != null && admin.getLockUntil().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("账号已锁定，请稍后再试");
            } else {
                // 锁定期已过，解除锁定
                admin.setStatus("normal");
                admin.setFailedLoginCount(0);
                admin.setLockUntil(null);
            }
        }

        // 验证密码
        if (!BCrypt.checkpw(password, admin.getPassword())) {
            // 密码错误，增加失败次数
            admin.setFailedLoginCount(admin.getFailedLoginCount() + 1);
            if (admin.getFailedLoginCount() >= 5) {
                // 失败5次，锁定30分钟
                admin.setStatus("locked");
                admin.setLockUntil(LocalDateTime.now().plusMinutes(30));
                adminMapper.updateById(admin);
                throw new RuntimeException("密码错误次数过多，账号已锁定30分钟");
            }
            adminMapper.updateById(admin);
            throw new RuntimeException("用户名或密码错误");
        }

        // 登录成功，重置失败次数
        admin.setFailedLoginCount(0);
        admin.setLastLoginTime(LocalDateTime.now());
        admin.setLastLoginIp(ip);
        adminMapper.updateById(admin);

        // 生成Token
        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("admin", admin);

        return result;
    }

    /**
     * 获取管理员信息
     */
    public Admin getAdminById(Integer id) {
        return adminMapper.selectById(id);
    }

    /**
     * 分页查询管理员列表
     */
    public PageResult<Admin> getAdminsByPage(Integer current, Integer size, String keyword, String status, String role) {
        Page<Admin> page = new Page<>(current, size);
        
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索（用户名）
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Admin::getUsername, keyword);
        }
        
        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(Admin::getStatus, status);
        }
        
        // 角色筛选
        if (StringUtils.hasText(role)) {
            wrapper.eq(Admin::getRole, role);
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Admin::getCreateTime);
        
        IPage<Admin> result = adminMapper.selectPage(page, wrapper);
        
        // 不返回密码
        result.getRecords().forEach(admin -> admin.setPassword(null));
        
        return new PageResult<>(result.getRecords(), result.getTotal(), 
                               result.getCurrent(), result.getSize());
    }

    /**
     * 创建管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createAdmin(String username, String password, String role) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        Long count = adminMapper.selectCount(wrapper);
        
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        Admin admin = new Admin();
        admin.setUsername(username);
        // 使用BCrypt加密密码
        admin.setPassword(BCrypt.hashpw(password));
        admin.setRole(role);
        admin.setStatus("normal");
        admin.setFailedLoginCount(0);
        
        return adminMapper.insert(admin) > 0;
    }

    /**
     * 更新管理员信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAdmin(Integer id, String username, String role) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 如果要修改用户名，检查是否与其他管理员重复
        if (StringUtils.hasText(username) && !username.equals(admin.getUsername())) {
            LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Admin::getUsername, username);
            wrapper.ne(Admin::getId, id);
            Long count = adminMapper.selectCount(wrapper);
            
            if (count > 0) {
                throw new RuntimeException("用户名已被使用");
            }
            
            admin.setUsername(username);
        }
        
        if (StringUtils.hasText(role)) {
            admin.setRole(role);
        }
        
        return adminMapper.updateById(admin) > 0;
    }

    /**
     * 删除管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAdmin(Integer id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 不允许删除超级管理员
        if ("super".equals(admin.getRole())) {
            throw new RuntimeException("不允许删除超级管理员");
        }
        
        return adminMapper.deleteById(id) > 0;
    }

    /**
     * 重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Integer id, String newPassword) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        admin.setPassword(BCrypt.hashpw(newPassword));
        admin.setFailedLoginCount(0);
        
        return adminMapper.updateById(admin) > 0;
    }

    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Integer id, String oldPassword, String newPassword) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 验证旧密码
        if (!BCrypt.checkpw(oldPassword, admin.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        admin.setPassword(BCrypt.hashpw(newPassword));
        
        return adminMapper.updateById(admin) > 0;
    }

    /**
     * 锁定/解锁账号
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLock(Integer id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 不允许锁定超级管理员
        if ("super".equals(admin.getRole())) {
            throw new RuntimeException("不允许锁定超级管理员");
        }
        
        if ("locked".equals(admin.getStatus())) {
            // 解锁
            admin.setStatus("normal");
            admin.setLockUntil(null);
            admin.setFailedLoginCount(0);
        } else {
            // 锁定
            admin.setStatus("locked");
            admin.setLockUntil(LocalDateTime.now().plusYears(10)); // 长期锁定
        }
        
        return adminMapper.updateById(admin) > 0;
    }

    /**
     * 获取所有管理员列表（不分页）
     */
    public List<Admin> getAllAdmins() {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Admin::getCreateTime);
        List<Admin> admins = adminMapper.selectList(wrapper);
        
        // 不返回密码
        admins.forEach(admin -> admin.setPassword(null));
        
        return admins;
    }
}

