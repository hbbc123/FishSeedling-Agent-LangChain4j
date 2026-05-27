package com.fishseedling.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 信息发布实体类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
@TableName(value = "tb_posts", autoResultMap = true)
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 信息类型：supply供应,purchase求购
     */
    private String type;

    /**
     * 鱼苗品种
     */
    private String fishBreed;

    /**
     * 规格
     */
    private String specification;

    /**
     * 数量
     */
    private String quantity;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 价格单位
     */
    private String priceUnit;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区县
     */
    private String district;

    /**
     * 详细地址
     */
    private String detailAddress;

    /**
     * 详细描述
     */
    private String description;

    /**
     * 图片路径数组（JSON格式）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> images;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话（加密）
     */
    private String contactPhone;

    /**
     * 管理码（6位随机码）
     */
    private String manageCode;

    /**
     * 管理码过期时间
     */
    private LocalDateTime manageCodeExpireTime;

    /**
     * 状态：pending待审核,approved已通过,rejected已拒绝,expired已过期,deleted已删除
     */
    private String status;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 是否置顶
     */
    private Boolean isTop;

    /**
     * 置顶过期时间
     */
    private LocalDateTime topExpireTime;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 电话查看次数
     */
    private Integer phoneViewCount;

    /**
     * 有效天数
     */
    private Integer validDays;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 发布者IP
     */
    private String publisherIp;

    /**
     * 设备指纹
     */
    private String publisherDevice;

    /**
     * 管理员备注
     */
    private String adminNotes;

    /**
     * 审核人ID
     */
    private Integer auditorId;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

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





