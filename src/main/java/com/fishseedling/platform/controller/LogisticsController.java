package com.fishseedling.platform.controller;

import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.dto.LogisticsPublishDTO;
import com.fishseedling.platform.dto.LogisticsQueryDTO;
import com.fishseedling.platform.entity.Logistics;
import com.fishseedling.platform.service.LogisticsService;
import com.fishseedling.platform.util.IpUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 物流信息Controller（前台）
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/front/logistics")
public class LogisticsController {

    @Resource
    private LogisticsService logisticsService;

    /**
     * 发布物流信息
     */
    @PostMapping("/publish")
    public Result<Map<String, Object>> publishLogistics(@Validated @RequestBody LogisticsPublishDTO dto,
                                                          HttpServletRequest request) {
        try {
            String ip = IpUtil.getIpAddress(request);
            String device = request.getHeader("User-Agent");
            Map<String, Object> result = logisticsService.publishLogistics(dto, ip, device);
            return Result.success("发布成功！您的物流信息已提交，通过审核后将在15分钟内上线展示。", result);
        } catch (Exception e) {
            return Result.error("发布失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询物流信息列表
     */
    @PostMapping("/list")
    public Result<PageResult<Logistics>> queryList(@RequestBody LogisticsQueryDTO queryDTO) {
        try {
            // 前台只查询已审核通过的物流信息，但保留其他搜索条件
            queryDTO.setStatus("approved");
            PageResult<Logistics> result = logisticsService.queryLogisticsByPage(queryDTO, true);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取物流详情
     */
    @GetMapping("/detail/{id}")
    public Result<Logistics> getDetail(@PathVariable Long id) {
        try {
            Logistics logistics = logisticsService.getLogisticsDetail(id);
            if (logistics == null) {
                return Result.error("物流信息不存在");
            }
            return Result.success(logistics);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 查看完整电话号码
     */
    @GetMapping("/phone/{id}")
    public Result<String> getFullPhone(@PathVariable Long id) {
        try {
            String phone = logisticsService.getFullPhone(id);
            if (phone == null) {
                return Result.error("物流信息不存在");
            }
            return Result.success(phone);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 通过管理码查询物流信息
     */
    @GetMapping("/manage/{manageCode}")
    public Result<Logistics> getByManageCode(@PathVariable String manageCode) {
        try {
            Logistics logistics = logisticsService.getLogisticsByManageCode(manageCode);
            if (logistics == null) {
                return Result.error("物流信息不存在或管理码已过期");
            }
            return Result.success(logistics);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 更新物流信息
     */
    @PutMapping("/update/{id}")
    public Result<Void> updateLogistics(@PathVariable Long id,
                                         @Validated @RequestBody LogisticsPublishDTO dto,
                                         @RequestHeader(value = "X-Manage-Code", required = false) String manageCode) {
        try {
            // 如果提供了管理码，则进行验证
            if (manageCode != null && !manageCode.isEmpty()) {
                boolean isValid = logisticsService.validateManageCode(id, manageCode);
                if (!isValid) {
                    return Result.error("管理码无效或已过期");
                }
            }
            
            boolean success = logisticsService.updateLogistics(id, dto);
            if (success) {
                Result<Void> result = Result.success("修改成功，物流信息将重新进入审核");
                return result;
            } else {
                return Result.error("修改失败");
            }
        } catch (Exception e) {
            return Result.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 删除物流信息
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteLogistics(@PathVariable Long id,
                                         @RequestHeader(value = "X-Manage-Code", required = false) String manageCode) {
        try {
            // 如果提供了管理码，则进行验证
            if (manageCode != null && !manageCode.isEmpty()) {
                boolean isValid = logisticsService.validateManageCode(id, manageCode);
                if (!isValid) {
                    return Result.error("管理码无效或已过期");
                }
            }
            
            boolean success = logisticsService.deleteLogistics(id);
            if (success) {
                Result<Void> result = Result.success("删除成功");
                return result;
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取首页物流信息
     */
    @GetMapping("/home")
    public Result<List<Logistics>> getHomePageLogistics() {
        try {
            List<Logistics> result = logisticsService.getHomePageLogistics();
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}

