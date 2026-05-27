package com.fishseedling.platform.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 物流信息查询DTO
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
public class LogisticsQueryDTO {

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long size = 10L;

    /**
     * 车辆类型
     */
    private String vehicleType;

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
     * 服务范围
     */
    private String serviceScope;

    /**
     * 是否有冷藏设备
     */
    private Boolean hasRefrigeration;

    /**
     * 是否有增氧设备
     */
    private Boolean hasOxygenSystem;

    /**
     * 最小载重量（吨）
     */
    private BigDecimal minLoad;

    /**
     * 最大载重量（吨）
     */
    private BigDecimal maxLoad;

    /**
     * 最小价格（元/公里）
     */
    private BigDecimal minPrice;

    /**
     * 最大价格（元/公里）
     */
    private BigDecimal maxPrice;

    /**
     * 状态
     */
    private String status;

    /**
     * 关键词搜索（车牌号、司机姓名、描述）
     */
    private String keyword;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 车牌号码
     */
    private String plateNumber;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 排序方式：createTime创建时间,viewCount浏览量,pricePerKm价格
     */
    private String orderBy;
}

