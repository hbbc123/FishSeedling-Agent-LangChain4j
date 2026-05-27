package com.fishseedling.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 地区实体类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Data
@TableName("tb_regions")
public class Region implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 行政区划代码
     */
    private String regionCode;

    /**
     * 地区名称
     */
    private String regionName;

    /**
     * 父级代码
     */
    private String parentCode;

    /**
     * 层级：1省,2市,3区
     */
    private Integer level;
}





