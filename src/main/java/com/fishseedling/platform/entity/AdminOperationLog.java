package com.fishseedling.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员操作日志实体类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
@TableName("tb_admin_operation_logs")
public class AdminOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 管理员ID
     */
    private Integer adminId;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作对象
     */
    private String operationObject;

    /**
     * 操作详情
     */
    private String operationDetail;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 操作时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}





