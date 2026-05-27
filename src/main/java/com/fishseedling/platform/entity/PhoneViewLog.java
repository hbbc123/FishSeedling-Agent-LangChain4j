package com.fishseedling.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 电话查看记录实体类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
@TableName("tb_phone_view_logs")
public class PhoneViewLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 信息ID
     */
    private Long postId;

    /**
     * 查看者IP
     */
    private String viewerIp;

    /**
     * 地理位置
     */
    private String viewerLocation;

    /**
     * 查看时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime viewTime;
}





