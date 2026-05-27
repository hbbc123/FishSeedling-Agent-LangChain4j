package com.fishseedling.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.dto.PostPublishDTO;
import com.fishseedling.platform.dto.PostQueryDTO;
import com.fishseedling.platform.entity.Post;
import com.fishseedling.platform.mapper.PostMapper;
import com.fishseedling.platform.util.EncryptUtil;
import com.fishseedling.platform.util.RandomUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 信息发布服务类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Service
public class PostService {

    @Resource
    private PostMapper postMapper;

    /**
     * 发布信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishPost(PostPublishDTO dto, String ip, String device) {
        // 创建实体
        Post post = new Post();
        BeanUtils.copyProperties(dto, post);

        // 加密电话号码
        post.setContactPhone(EncryptUtil.aesEncrypt(dto.getContactPhone()));

        // 生成管理码（6位）
        String manageCode = RandomUtil.generateManageCode();
        post.setManageCode(manageCode);

        // 管理码有效期24小时
        post.setManageCodeExpireTime(LocalDateTime.now().plusHours(24));

        // 设置状态为待审核
        post.setStatus("pending");

        // 设置过期时间
        post.setExpireTime(LocalDateTime.now().plusDays(dto.getValidDays()));

        // 设置发布者信息
        post.setPublisherIp(ip);
        post.setPublisherDevice(device);

        // 初始化计数
        post.setViewCount(0);
        post.setPhoneViewCount(0);
        post.setIsTop(false);

        // 插入数据库
        postMapper.insert(post);

        // 返回信息ID和管理码
        Map<String, Object> result = new HashMap<>();
        result.put("id", post.getId());
        result.put("manageCode", manageCode);
        return result;
    }

    /**
     * 多条件分页查询（前台用，电话脱敏）
     */
    public PageResult<Post> queryPostsByPage(PostQueryDTO queryDTO) {
        return queryPostsByPage(queryDTO, true);
    }

    /**
     * 多条件分页查询（可控制是否脱敏）
     * @param queryDTO 查询条件
     * @param desensitize 是否脱敏（true=脱敏，false=完整显示）
     */
    public PageResult<Post> queryPostsByPage(PostQueryDTO queryDTO, boolean desensitize) {
        // 构建分页对象
        Page<Post> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 构建查询参数
        Map<String, Object> params = new HashMap<>();
        if (queryDTO.getType() != null && !queryDTO.getType().isEmpty()) {
            params.put("type", queryDTO.getType());
        }
        if (queryDTO.getFishBreed() != null && !queryDTO.getFishBreed().isEmpty()) {
            params.put("fishBreed", queryDTO.getFishBreed());
        }
        if (queryDTO.getProvince() != null && !queryDTO.getProvince().isEmpty()) {
            params.put("province", queryDTO.getProvince());
        }
        if (queryDTO.getCity() != null && !queryDTO.getCity().isEmpty()) {
            params.put("city", queryDTO.getCity());
        }
        if (queryDTO.getDistrict() != null && !queryDTO.getDistrict().isEmpty()) {
            params.put("district", queryDTO.getDistrict());
        }
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
            params.put("status", queryDTO.getStatus());
        }
        if (queryDTO.getMinPrice() != null) {
            params.put("minPrice", queryDTO.getMinPrice());
        }
        if (queryDTO.getMaxPrice() != null) {
            params.put("maxPrice", queryDTO.getMaxPrice());
        }
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            params.put("keyword", queryDTO.getKeyword());
        }
        if (queryDTO.getContactPerson() != null && !queryDTO.getContactPerson().isEmpty()) {
            params.put("contactPerson", queryDTO.getContactPerson());
        }
        if (queryDTO.getContactPhone() != null && !queryDTO.getContactPhone().isEmpty()) {
            params.put("contactPhone", queryDTO.getContactPhone());
        }
        if (queryDTO.getStartTime() != null && !queryDTO.getStartTime().isEmpty()) {
            params.put("startTime", queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null && !queryDTO.getEndTime().isEmpty()) {
            params.put("endTime", queryDTO.getEndTime());
        }
        if (queryDTO.getOrderBy() != null && !queryDTO.getOrderBy().isEmpty()) {
            params.put("orderBy", queryDTO.getOrderBy());
        }
        if (queryDTO.getPriceUnit() != null && !queryDTO.getPriceUnit().isEmpty()) {
            params.put("priceUnit", queryDTO.getPriceUnit());
        }

        // 执行查询
        IPage<Post> result = postMapper.selectPageByConditions(page, params);

        // 处理电话号码（根据参数决定是否脱敏）
        List<Post> records = result.getRecords().stream().map(post -> {
            String phone = EncryptUtil.aesDecrypt(post.getContactPhone());
            post.setContactPhone(phone);
            return post;
        }).collect(Collectors.toList());

        // 处理电话号码（根据参数决定是否脱敏）
        List<Post> records1 = records.stream().map(post -> {
            String phone = EncryptUtil.aesDecrypt(post.getManageCode());
            post.setContactPhone(phone);
            return post;
        }).collect(Collectors.toList());

        return new PageResult<>(records1, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 获取信息详情
     */
    public Post getPostDetail(Long id) {
        // 使用自定义查询方法确保JSON字段正确处理
        Post post = postMapper.selectDetailById(id);
        if (post != null) {
            // 脱敏处理电话号码
            String phone = EncryptUtil.aesDecrypt(post.getContactPhone());
            post.setContactPhone(EncryptUtil.desensitizePhone(phone));

            // 增加浏览次数
            post.setViewCount(post.getViewCount() + 1);
            postMapper.updateById(post);
        }
        return post;
    }

    /**
     * 查看完整电话号码
     */
    public String getFullPhone(Long id) {
        Post post = postMapper.selectById(id);
        if (post != null) {
            // 解密电话号码
            String phone = EncryptUtil.aesDecrypt(post.getContactPhone());

            // 增加电话查看次数
            post.setPhoneViewCount(post.getPhoneViewCount() + 1);
            postMapper.updateById(post);

            return phone;
        }
        return null;
    }

    /**
     * 通过管理码查询信息
     */
    public Post getPostByManageCode(String manageCode) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getManageCode, manageCode);
        Post post = postMapper.selectOne(wrapper);
        if (post != null) {
            // 检查管理码是否过期
            if (post.getManageCodeExpireTime().isBefore(LocalDateTime.now())) {
                return null;
            }
            // 解密电话号码
            post.setContactPhone(EncryptUtil.aesDecrypt(post.getContactPhone()));
            
            // 由于使用selectOne可能不会正确处理JSON字段，我们重新用ID查一次确保获取到images
            Post detailPost = postMapper.selectDetailById(post.getId());
            if (detailPost != null) {
                detailPost.setContactPhone(post.getContactPhone()); // 保持解密后的电话
                return detailPost;
            }
        }
        return post;
    }

    /**
     * 验证管理码
     */
    public boolean validateManageCode(Long postId, String manageCode) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            return false;
        }
        
        // 检查管理码是否匹配
        if (!manageCode.equals(post.getManageCode())) {
            return false;
        }
        
        // 检查管理码是否过期
        if (post.getManageCodeExpireTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        return true;
    }

    /**
     * 更新信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePost(Long id, PostPublishDTO dto) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return false;
        }

        // 更新字段
        BeanUtils.copyProperties(dto, post);

        // 加密电话号码
        post.setContactPhone(EncryptUtil.aesEncrypt(dto.getContactPhone()));

        // 重新设置为待审核
        post.setStatus("pending");

        return postMapper.updateById(post) > 0;
    }

    /**
     * 删除信息（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return false;
        }

        post.setStatus("deleted");
        return postMapper.updateById(post) > 0;
    }

    /**
     * 审核信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean auditPost(Long id, String status, String rejectReason, Integer auditorId) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return false;
        }

        post.setStatus(status);
        post.setRejectReason(rejectReason);
        post.setAuditorId(auditorId);
        post.setAuditTime(LocalDateTime.now());

        return postMapper.updateById(post) > 0;
    }

    /**
     * 置顶信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean topPost(Long id, Integer days) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return false;
        }

        post.setIsTop(true);
        if (days > 0) {
            post.setTopExpireTime(LocalDateTime.now().plusDays(days));
        }

        return postMapper.updateById(post) > 0;
    }

    /**
     * 取消置顶
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTop(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            return false;
        }

        post.setIsTop(false);
        post.setTopExpireTime(null);

        return postMapper.updateById(post) > 0;
    }

    /**
     * 获取首页最新信息
     */
    public Map<String, Object> getHomePageData() {
        Map<String, Object> result = new HashMap<>();

        // 获取最新供应信息（8条）
        LambdaQueryWrapper<Post> supplyWrapper = new LambdaQueryWrapper<>();
        supplyWrapper.eq(Post::getType, "supply")
                .eq(Post::getStatus, "approved")
                .orderByDesc(Post::getIsTop, Post::getCreateTime)
                .last("LIMIT 8");
        List<Post> supplyList = postMapper.selectList(supplyWrapper);
        supplyList.forEach(post -> {
            String phone = EncryptUtil.aesDecrypt(post.getContactPhone());
            post.setContactPhone(EncryptUtil.desensitizePhone(phone));
        });

        // 获取最新求购信息（8条）
        LambdaQueryWrapper<Post> purchaseWrapper = new LambdaQueryWrapper<>();
        purchaseWrapper.eq(Post::getType, "purchase")
                .eq(Post::getStatus, "approved")
                .orderByDesc(Post::getIsTop, Post::getCreateTime)
                .last("LIMIT 8");
        List<Post> purchaseList = postMapper.selectList(purchaseWrapper);
        purchaseList.forEach(post -> {
            String phone = EncryptUtil.aesDecrypt(post.getContactPhone());
            post.setContactPhone(EncryptUtil.desensitizePhone(phone));
        });

        result.put("supplyList", supplyList);
        result.put("purchaseList", purchaseList);

        return result;
    }
}

