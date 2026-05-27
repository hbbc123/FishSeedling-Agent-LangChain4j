package com.fishseedling.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帮助内容实体类
 * 用于存储"使用帮助"模块的动态内容
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
@Data
@TableName("tb_help_content")
public class HelpContent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类：
     * quick-start - 快速入门
     * publish-guide - 发布指南
     * safety-tips - 安全须知
     * contact-us - 联系客服
     */
    private String category;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容（支持 HTML 富文本）
     */
    private String content;

    /**
     * 排序号（数字越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 状态：enabled-启用, disabled-禁用
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
