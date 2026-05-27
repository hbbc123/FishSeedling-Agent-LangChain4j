package com.fishseedling.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.dto.LogisticsPublishDTO;
import com.fishseedling.platform.dto.LogisticsQueryDTO;
import com.fishseedling.platform.entity.Logistics;
import com.fishseedling.platform.mapper.LogisticsMapper;
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
 * 物流信息服务类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Service
public class LogisticsService {

    @Resource
    private LogisticsMapper logisticsMapper;

    /**
     * 发布物流信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishLogistics(LogisticsPublishDTO dto, String ip, String device) {
        // 创建实体
        Logistics logistics = new Logistics();
        BeanUtils.copyProperties(dto, logistics);

        // 加密电话号码
        logistics.setContactPhone(EncryptUtil.aesEncrypt(dto.getContactPhone()));

        // 生成管理码（6位）
        String manageCode = RandomUtil.generateManageCode();
        logistics.setManageCode(manageCode);

        // 管理码有效期24小时
        logistics.setManageCodeExpireTime(LocalDateTime.now().plusHours(24));

        // 设置状态为待审核
        logistics.setStatus("pending");

        // 设置过期时间
        logistics.setExpireTime(LocalDateTime.now().plusDays(dto.getValidDays()));

        // 设置发布者信息
        logistics.setPublisherIp(ip);
        logistics.setPublisherDevice(device);

        // 初始化计数
        logistics.setViewCount(0);
        logistics.setPhoneViewCount(0);
        logistics.setIsTop(false);

        // 插入数据库
        logisticsMapper.insert(logistics);

        // 返回信息ID和管理码
        Map<String, Object> result = new HashMap<>();
        result.put("id", logistics.getId());
        result.put("manageCode", manageCode);
        return result;
    }

    /**
     * 多条件分页查询（前台用，电话脱敏）
     */
    public PageResult<Logistics> queryLogisticsByPage(LogisticsQueryDTO queryDTO) {
        return queryLogisticsByPage(queryDTO, true);
    }

    /**
     * 多条件分页查询（可控制是否脱敏）
     * @param queryDTO 查询条件
     * @param desensitize 是否脱敏（true=脱敏，false=完整显示）
     */
    public PageResult<Logistics> queryLogisticsByPage(LogisticsQueryDTO queryDTO, boolean desensitize) {
        // 构建分页对象
        Page<Logistics> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 构建查询参数
        Map<String, Object> params = new HashMap<>();
        if (queryDTO.getVehicleType() != null && !queryDTO.getVehicleType().isEmpty()) {
            params.put("vehicleType", queryDTO.getVehicleType());
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
        if (queryDTO.getServiceScope() != null && !queryDTO.getServiceScope().isEmpty()) {
            params.put("serviceScope", queryDTO.getServiceScope());
        }
        if (queryDTO.getHasRefrigeration() != null) {
            params.put("hasRefrigeration", queryDTO.getHasRefrigeration());
        }
        if (queryDTO.getHasOxygenSystem() != null) {
            params.put("hasOxygenSystem", queryDTO.getHasOxygenSystem());
        }
        if (queryDTO.getMinLoad() != null) {
            params.put("minLoad", queryDTO.getMinLoad());
        }
        if (queryDTO.getMaxLoad() != null) {
            params.put("maxLoad", queryDTO.getMaxLoad());
        }
        if (queryDTO.getMinPrice() != null) {
            params.put("minPrice", queryDTO.getMinPrice());
        }
        if (queryDTO.getMaxPrice() != null) {
            params.put("maxPrice", queryDTO.getMaxPrice());
        }
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
            params.put("status", queryDTO.getStatus());
        }
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            params.put("keyword", queryDTO.getKeyword());
        }
        if (queryDTO.getDriverName() != null && !queryDTO.getDriverName().isEmpty()) {
            params.put("driverName", queryDTO.getDriverName());
        }
        if (queryDTO.getPlateNumber() != null && !queryDTO.getPlateNumber().isEmpty()) {
            params.put("plateNumber", queryDTO.getPlateNumber());
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

        // 执行查询
        IPage<Logistics> result = logisticsMapper.selectPageByConditions(page, params);

        // 处理电话号码（根据参数决定是否脱敏）
        List<Logistics> records = result.getRecords().stream().map(logistics -> {
            String phone = EncryptUtil.aesDecrypt(logistics.getContactPhone());
            if (desensitize) {
                logistics.setContactPhone(EncryptUtil.desensitizePhone(phone));
            } else {
                logistics.setContactPhone(phone);
            }
            return logistics;
        }).collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 获取物流详情
     */
    public Logistics getLogisticsDetail(Long id) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics != null) {
            // 脱敏处理电话号码
            String phone = EncryptUtil.aesDecrypt(logistics.getContactPhone());
            logistics.setContactPhone(EncryptUtil.desensitizePhone(phone));

            // 增加浏览次数
            logistics.setViewCount(logistics.getViewCount() + 1);
            logisticsMapper.updateById(logistics);
        }
        return logistics;
    }

    /**
     * 查看完整电话号码
     */
    public String getFullPhone(Long id) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics != null) {
            // 解密电话号码
            String phone = EncryptUtil.aesDecrypt(logistics.getContactPhone());

            // 增加电话查看次数
            logistics.setPhoneViewCount(logistics.getPhoneViewCount() + 1);
            logisticsMapper.updateById(logistics);

            return phone;
        }
        return null;
    }

    /**
     * 通过管理码查询物流信息
     */
    public Logistics getLogisticsByManageCode(String manageCode) {
        LambdaQueryWrapper<Logistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Logistics::getManageCode, manageCode);
        Logistics logistics = logisticsMapper.selectOne(wrapper);
        if (logistics != null) {
            // 检查管理码是否过期
            if (logistics.getManageCodeExpireTime().isBefore(LocalDateTime.now())) {
                return null;
            }
            // 解密电话号码
            logistics.setContactPhone(EncryptUtil.aesDecrypt(logistics.getContactPhone()));
        }
        return logistics;
    }

    /**
     * 验证管理码
     */
    public boolean validateManageCode(Long logisticsId, String manageCode) {
        Logistics logistics = logisticsMapper.selectById(logisticsId);
        if (logistics == null) {
            return false;
        }
        
        // 检查管理码是否匹配
        if (!manageCode.equals(logistics.getManageCode())) {
            return false;
        }
        
        // 检查管理码是否过期
        if (logistics.getManageCodeExpireTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        return true;
    }

    /**
     * 更新物流信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLogistics(Long id, LogisticsPublishDTO dto) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            return false;
        }

        // 更新字段
        BeanUtils.copyProperties(dto, logistics);

        // 加密电话号码
        logistics.setContactPhone(EncryptUtil.aesEncrypt(dto.getContactPhone()));

        // 重新设置为待审核
        logistics.setStatus("pending");

        return logisticsMapper.updateById(logistics) > 0;
    }

    /**
     * 删除物流信息（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLogistics(Long id) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            return false;
        }

        logistics.setStatus("deleted");
        return logisticsMapper.updateById(logistics) > 0;
    }

    /**
     * 审核物流信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean auditLogistics(Long id, String status, String rejectReason, Integer auditorId) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            return false;
        }

        logistics.setStatus(status);
        logistics.setRejectReason(rejectReason);
        logistics.setAuditorId(auditorId);
        logistics.setAuditTime(LocalDateTime.now());

        return logisticsMapper.updateById(logistics) > 0;
    }

    /**
     * 置顶物流信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean topLogistics(Long id, Integer days) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            return false;
        }

        logistics.setIsTop(true);
        if (days > 0) {
            logistics.setTopExpireTime(LocalDateTime.now().plusDays(days));
        }

        return logisticsMapper.updateById(logistics) > 0;
    }

    /**
     * 取消置顶
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTop(Long id) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            return false;
        }

        logistics.setIsTop(false);
        logistics.setTopExpireTime(null);

        return logisticsMapper.updateById(logistics) > 0;
    }

    /**
     * 获取首页最新物流信息
     */
    public List<Logistics> getHomePageLogistics() {
        LambdaQueryWrapper<Logistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Logistics::getStatus, "approved")
                .orderByDesc(Logistics::getIsTop, Logistics::getCreateTime)
                .last("LIMIT 8");
        List<Logistics> list = logisticsMapper.selectList(wrapper);
        list.forEach(logistics -> {
            String phone = EncryptUtil.aesDecrypt(logistics.getContactPhone());
            logistics.setContactPhone(EncryptUtil.desensitizePhone(phone));
        });
        return list;
    }
}

