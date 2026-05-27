package com.fishseedling.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员实体类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
@TableName("tb_admins")
public class Admin implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 角色：super超级管理员,normal普通管理员
     */
    private String role;

    /**
     * 状态：normal正常,locked锁定
     */
    private String status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 登录失败次数
     */
    private Integer failedLoginCount;

    /**
     * 锁定截止时间
     */
    private LocalDateTime lockUntil;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}





