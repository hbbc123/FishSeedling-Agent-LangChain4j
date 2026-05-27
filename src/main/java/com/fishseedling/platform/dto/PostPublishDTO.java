package com.fishseedling.platform.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 信息发布DTO
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
public class PostPublishDTO {

    /**
     * 信息类型
     */
    @NotBlank(message = "信息类型不能为空")
    private String type;

    /**
     * 鱼苗品种
     */
    @NotBlank(message = "鱼苗品种不能为空")
    private String fishBreed;

    /**
     * 规格
     */
    @NotBlank(message = "规格不能为空")
    private String specification;

    /**
     * 数量
     */
    @NotBlank(message = "数量不能为空")
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
    @NotBlank(message = "省份不能为空")
    private String province;

    /**
     * 城市
     */
    @NotBlank(message = "城市不能为空")
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
     * 图片路径列表
     */
    private List<String> images;

    /**
     * 联系人
     */
    @NotBlank(message = "联系人不能为空")
    private String contactPerson;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    /**
     * 有效天数
     */
    @NotNull(message = "有效天数不能为空")
    private Integer validDays = 30;
}





