package com.fishseedling.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fishseedling.platform.entity.HelpContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 帮助内容 Mapper
 *
 * @author Fish Seedling Platform
 * @since 2026-05-24
 */
@Mapper
public interface HelpContentMapper extends BaseMapper<HelpContent> {

    /**
     * 根据分类查询启用的帮助内容（按排序号升序）
     */
    @Select("SELECT * FROM tb_help_content WHERE category = #{category} AND status = 'enabled' ORDER BY sort_order ASC")
    List<HelpContent> selectByCategoryEnabled(@Param("category") String category);

    /**
     * 查询所有启用的帮助内容（按分类和排序号）
     */
    @Select("SELECT * FROM tb_help_content WHERE status = 'enabled' ORDER BY category, sort_order ASC")
    List<HelpContent> selectAllEnabled();
}
