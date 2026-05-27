package com.fishseedling.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 养殖手册实体类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
@TableName("tb_handbooks")
public class Handbook implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 鱼种名称
     */
    private String fishName;

    /**
     * 别名
     */
    private String fishAlias;

    /**
     * 分类：freshwater淡水鱼,saltwater海水鱼,shrimp_crab虾蟹类,special特种水产
     */
    private String category;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 简介
     */
    private String briefIntro;

    /**
     * 养殖难度：easy简单,medium中等,hard困难
     */
    private String difficulty;

    /**
     * 内容（富文本HTML）
     */
    private String content;

    /**
     * 是否发布
     */
    private int isPublished;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 浏览次数
     */
    private Integer viewCount;

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





