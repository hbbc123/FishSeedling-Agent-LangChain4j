package com.fishseedling.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fishseedling.platform.entity.Logistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 物流信息Mapper
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Mapper
public interface LogisticsMapper extends BaseMapper<Logistics> {

    /**
     * 多条件分页查询
     *
     * @param page 分页对象
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<Logistics> selectPageByConditions(Page<Logistics> page, @Param("params") Map<String, Object> params);

    /**
     * 按天统计物流信息发布数量（最近30天）
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count " +
            "FROM tb_logistics " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY DATE(create_time)")
    List<Map<String, Object>> selectDailyLogisticsCount();
}

