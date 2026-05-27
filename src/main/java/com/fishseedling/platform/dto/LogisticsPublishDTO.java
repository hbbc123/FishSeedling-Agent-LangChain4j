package com.fishseedling.platform.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 物流信息发布DTO
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
public class LogisticsPublishDTO {

    /**
     * 车辆类型：冷藏车/水罐车/普通货车
     */
    @NotBlank(message = "车辆类型不能为空")
    private String vehicleType;

    /**
     * 车牌号码
     */
    @NotBlank(message = "车牌号码不能为空")
    @Pattern(regexp = "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$", 
             message = "车牌号码格式不正确")
    private String plateNumber;

    /**
     * 最大载重量（吨）
     */
    @NotNull(message = "最大载重量不能为空")
    @DecimalMin(value = "0.1", message = "最大载重量必须大于0")
    private BigDecimal maxLoad;

    /**
     * 常驻省份
     */
    @NotBlank(message = "常驻省份不能为空")
    private String province;

    /**
     * 常驻城市
     */
    @NotBlank(message = "常驻城市不能为空")
    private String city;

    /**
     * 常驻区县
     */
    private String district;

    /**
     * 司机姓名
     */
    @NotBlank(message = "司机姓名不能为空")
    private String driverName;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    /**
     * 每公里单价（元）
     */
    @NotNull(message = "每公里单价不能为空")
    @DecimalMin(value = "0.01", message = "每公里单价必须大于0")
    private BigDecimal pricePerKm;

    /**
     * 起步价（元）
     */
    @NotNull(message = "起步价不能为空")
    @DecimalMin(value = "0", message = "起步价不能为负数")
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
    @Min(value = 0, message = "车龄不能为负数")
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
    @NotNull(message = "有效天数不能为空")
    @Min(value = 15, message = "有效天数不能少于15天")
    @Max(value = 60, message = "有效天数不能超过60天")
    private Integer validDays;

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
     * 车辆图片（最多5张）
     */
    @Size(max = 5, message = "最多只能上传5张图片")
    private List<String> images;
}

