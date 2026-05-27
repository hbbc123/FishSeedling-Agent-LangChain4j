package com.fishseedling.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流信息实体类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
@TableName(value = "tb_logistics", autoResultMap = true)
public class Logistics {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 车辆类型：冷藏车/水罐车/普通货车
     */
    private String vehicleType;

    /**
     * 车牌号码
     */
    private String plateNumber;

    /**
     * 最大载重量（吨）
     */
    private BigDecimal maxLoad;

    /**
     * 常驻省份
     */
    private String province;

    /**
     * 常驻城市
     */
    private String city;

    /**
     * 常驻区县
     */
    private String district;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 联系电话（加密）
     */
    private String contactPhone;

    /**
     * 每公里单价（元）
     */
    private BigDecimal pricePerKm;

    /**
     * 起步价（元）
     */
    private BigDecimal basePrice;

    /**
     * 起步公里数
     */
    private BigDecimal baseKm;

    /**
     * 车辆品牌/型号
     */
    private String vehicleBrand;

    /**
     * 车厢尺寸（长×宽×高，米）
     */
    private String vehicleSize;

    /**
     * 车龄（年）
     */
    private Integer vehicleAge;

    /**
     * 营运证号
     */
    private String operationLicense;

    /**
     * 建议装载量（吨）
     */
    private BigDecimal suggestedLoad;

    /**
     * 制氧设备类型
     */
    private String oxygenEquipment;

    /**
     * 制氧设备数量
     */
    private Integer oxygenCount;

    /**
     * 制氧设备功率
     */
    private String oxygenPower;

    /**
     * 是否有冷藏设备
     */
    private Boolean hasRefrigeration;

    /**
     * 温控范围（℃）
     */
    private String tempRange;

    /**
     * 是否有保温措施
     */
    private Boolean hasInsulation;

    /**
     * 是否配备增氧设备
     */
    private Boolean hasOxygenSystem;

    /**
     * 是否有水循环系统
     */
    private Boolean hasWaterCirculation;

    /**
     * 水箱容量（立方米）
     */
    private BigDecimal waterTankCapacity;

    /**
     * 服务范围：市内/省内/跨省
     */
    private String serviceScope;

    /**
     * 可配送区域（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> serviceAreas;

    /**
     * 是否接受长途运输
     */
    private Boolean acceptLongDistance;

    /**
     * 不服务的区域
     */
    private String excludedAreas;

    /**
     * 计价单位
     */
    private String priceUnit;

    /**
     * 是否包含过路费
     */
    private Boolean includeToll;

    /**
     * 是否包含油费
     */
    private Boolean includeFuel;

    /**
     * 是否收取空返费用
     */
    private Boolean chargeEmptyReturn;

    /**
     * 空返费率（%）
     */
    private BigDecimal emptyReturnRate;

    /**
     * 夜间加价比例（%）
     */
    private BigDecimal nightExtraRate;

    /**
     * 节假日加价比例（%）
     */
    private BigDecimal holidayExtraRate;

    /**
     * 单次运输最低金额（元）
     */
    private BigDecimal minOrderAmount;

    /**
     * 是否接受议价
     */
    private Boolean negotiable;

    /**
     * 驾龄（年）
     */
    private Integer drivingYears;

    /**
     * 驾驶证类型：A1/A2/B1/B2等
     */
    private String licenseType;

    /**
     * 从业年限（运输鱼苗经验）
     */
    private Integer workYears;

    /**
     * 是否购买货物运输保险
     */
    private Boolean hasInsurance;

    /**
     * 承诺的鱼苗存活率（%）
     */
    private BigDecimal survivalRate;

    /**
     * 可出车时间：随时/预约/固定时段
     */
    private String availableTime;

    /**
     * 最快响应时间
     */
    private String responseTime;

    /**
     * 是否接急单
     */
    private Boolean acceptUrgent;

    /**
     * 有效天数
     */
    private Integer validDays;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否提供装卸服务
     */
    private Boolean serviceLoading;

    /**
     * 是否协助打氧
     */
    private Boolean serviceOxygen;

    /**
     * 是否提供包装材料
     */
    private Boolean servicePackaging;

    /**
     * 是否支持代收货款
     */
    private Boolean serviceCod;

    /**
     * 是否提供GPS实时跟踪
     */
    private Boolean serviceGps;

    /**
     * 付款方式：先付/到付/账期
     */
    private String paymentMethod;

    /**
     * 详细说明
     */
    private String description;

    /**
     * 特殊要求
     */
    private String specialRequirements;

    /**
     * 车辆图片（JSON数组，最多5张）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

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
