package com.fishseedling.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fishseedling.platform.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 信息发布Mapper
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 多条件分页查询（动态SQL，空字段不查询）
     *
     * @param page   分页对象
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<Post> selectPageByConditions(Page<Post> page, @Param("params") Map<String, Object> params);

    /**
     * 根据ID查询详情（使用ResultMap确保JSON字段正确处理）
     *
     * @param id 主键ID
     * @return Post对象
     */
    Post selectDetailById(@Param("id") Long id);

    /**
     * 按天统计信息发布数量（最近30天）
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count " +
            "FROM tb_posts " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY DATE(create_time)")
    List<Map<String, Object>> selectDailyPostCount();
}

