package com.fishseedling.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报实体类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
@TableName("tb_reports")
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 被举报对象类型：post信息,logistics物流
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 被举报对象ID
     */
    @TableField("target_id") 
    private Long targetId;

    /**
     * 举报原因：虚假信息,重复发布,信息过时,联系方式错误,价格异常,其他问题
     */
    @TableField("reason")
    private String reason;

    /**
     * 举报描述
     */
    @TableField("description")
    private String description;

    /**
     * 举报人联系方式
     */
    private String reporterContact;

    /**
     * 举报人IP
     */
    private String reporterIp;

    /**
     * 处理状态：pending待处理,processed已处理,ignored已忽略
     */
    private String status;

    /**
     * 处理结果
     */
    private String processResult;

    /**
     * 处理人ID
     */
    private Integer processorId;

    /**
     * 处理时间
     */
    private LocalDateTime processTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}





