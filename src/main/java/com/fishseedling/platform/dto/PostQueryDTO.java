package com.fishseedling.platform.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 信息查询DTO
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
public class PostQueryDTO {

    /**
     * 当前页码
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long size = 20L;

    /**
     * 信息类型
     */
    private String type;

    /**
     * 鱼苗品种
     */
    private String fishBreed;

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
     * 状态
     */
    private String status;

    /**
     * 最小价格
     */
    private BigDecimal minPrice;

    /**
     * 最大价格
     */
    private BigDecimal maxPrice;


    /**
     * 价格单位：元/尾、元/斤、元/公斤、元/只
     */
    private String  priceUnit;

    /**
     * 关键词搜索
     */
    private String keyword;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 排序方式
     */
    private String orderBy;
}





