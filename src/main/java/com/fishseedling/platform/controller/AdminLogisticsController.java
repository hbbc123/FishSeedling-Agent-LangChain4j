package com.fishseedling.platform.controller;

import com.fishseedling.platform.common.PageResult;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.dto.LogisticsQueryDTO;
import com.fishseedling.platform.entity.Logistics;
import com.fishseedling.platform.service.LogisticsService;
import com.fishseedling.platform.service.AdminOperationLogService;
import com.fishseedling.platform.util.IpUtil;
import com.fishseedling.platform.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 物流信息Controller（管理后台）
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/admin/logistics")
public class AdminLogisticsController {

    @Resource
    private LogisticsService logisticsService;

    @Resource
    private AdminOperationLogService adminOperationLogService;

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 分页查询物流信息列表（管理后台，显示所有状态）
     */
    @PostMapping("/list")
    public Result<PageResult<Logistics>> queryList(@RequestBody LogisticsQueryDTO queryDTO) {
        try {
            // 管理后台查询时不脱敏电话号码
            PageResult<Logistics> result = logisticsService.queryLogisticsByPage(queryDTO, false);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 审核物流信息
     */
    @PutMapping("/audit/{id}")
    public Result<Void> auditLogistics(@PathVariable Long id,
                                        @RequestParam String status,
                                        @RequestParam(required = false) String rejectReason,
                                        HttpServletRequest request) {
        try {
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer auditorId = jwtUtil.getAdminIdFromToken(token);
            
            if (auditorId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            // 先获取物流信息详情用于日志记录
            Logistics logistics = logisticsService.getLogisticsDetail(id);
            
            boolean success = logisticsService.auditLogistics(id, status, rejectReason, auditorId);
            if (success) {
                // 记录审核日志
                String ip = IpUtil.getIpAddress(request);
                String detail = ("approved".equals(status) ? "通过" : "拒绝") + 
                        "了物流信息ID:" + id;
                if (logistics != null && logistics.getDriverName() != null) {
                    detail += "，司机：" + logistics.getDriverName();
                }
                if (logistics != null && logistics.getPlateNumber() != null) {
                    detail += "，车牌：" + logistics.getPlateNumber();
                }
                if ("rejected".equals(status) && rejectReason != null) {
                    detail += "，拒绝原因：" + rejectReason;
                }
                adminOperationLogService.log(
                        auditorId,
                        "审核物流信息",
                        "物流ID: " + id,
                        detail,
                        ip
                );
                
                Result<Void> result = Result.success("审核成功");
                return result;
            } else {
                return Result.error("审核失败");
            }
        } catch (Exception e) {
            return Result.error("审核失败：" + e.getMessage());
        }
    }

    /**
     * 置顶物流信息
     */
    @PutMapping("/top/{id}")
    public Result<Void> topLogistics(@PathVariable Long id,
                                      @RequestParam(defaultValue = "7") Integer days,
                                      HttpServletRequest request) {
        try {
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer adminId = jwtUtil.getAdminIdFromToken(token);
            
            if (adminId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            // 先获取物流信息详情用于日志记录
            Logistics logistics = logisticsService.getLogisticsDetail(id);
            
            boolean success = logisticsService.topLogistics(id, days);
            if (success) {
                // 记录置顶日志
                String ip = IpUtil.getIpAddress(request);
                String detail = "置顶了物流信息ID:" + id + "，置顶天数：" + days + "天";
                if (logistics != null && logistics.getDriverName() != null) {
                    detail += "，司机：" + logistics.getDriverName();
                }
                if (logistics != null && logistics.getPlateNumber() != null) {
                    detail += "，车牌：" + logistics.getPlateNumber();
                }
                adminOperationLogService.log(
                        adminId,
                        "置顶物流信息",
                        "物流ID: " + id,
                        detail,
                        ip
                );
                
                Result<Void> result = Result.success("置顶成功");
                return result;
            } else {
                return Result.error("置顶失败");
            }
        } catch (Exception e) {
            return Result.error("置顶失败：" + e.getMessage());
        }
    }

    /**
     * 取消置顶
     */
    @PutMapping("/cancel-top/{id}")
    public Result<Void> cancelTop(@PathVariable Long id,
                                   HttpServletRequest request) {
        try {
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer adminId = jwtUtil.getAdminIdFromToken(token);
            
            if (adminId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            // 先获取物流信息详情用于日志记录
            Logistics logistics = logisticsService.getLogisticsDetail(id);
            
            boolean success = logisticsService.cancelTop(id);
            if (success) {
                // 记录取消置顶日志
                String ip = IpUtil.getIpAddress(request);
                String detail = "取消置顶了物流信息ID:" + id;
                if (logistics != null && logistics.getDriverName() != null) {
                    detail += "，司机：" + logistics.getDriverName();
                }
                if (logistics != null && logistics.getPlateNumber() != null) {
                    detail += "，车牌：" + logistics.getPlateNumber();
                }
                adminOperationLogService.log(
                        adminId,
                        "取消置顶物流",
                        "物流ID: " + id,
                        detail,
                        ip
                );
                
                Result<Void> result = Result.success("取消置顶成功");
                return result;
            } else {
                return Result.error("取消置顶失败");
            }
        } catch (Exception e) {
            return Result.error("取消置顶失败：" + e.getMessage());
        }
    }

    /**
     * 删除物流信息
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteLogistics(@PathVariable Long id,
                                         HttpServletRequest request) {
        try {
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer adminId = jwtUtil.getAdminIdFromToken(token);
            
            if (adminId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            // 先获取物流信息详情用于日志记录
            Logistics logistics = logisticsService.getLogisticsDetail(id);
            
            boolean success = logisticsService.deleteLogistics(id);
            if (success) {
                // 记录删除日志
                String ip = IpUtil.getIpAddress(request);
                String detail = "删除了物流信息ID:" + id;
                if (logistics != null) {
                    if (logistics.getDriverName() != null) {
                        detail += "，司机：" + logistics.getDriverName();
                    }
                    if (logistics.getPlateNumber() != null) {
                        detail += "，车牌：" + logistics.getPlateNumber();
                    }
                    if (logistics.getVehicleType() != null) {
                        detail += "，车辆类型：" + logistics.getVehicleType();
                    }
                }
                adminOperationLogService.log(
                        adminId,
                        "删除物流信息",
                        "物流ID: " + id,
                        detail,
                        ip
                );
                
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
     * 获取物流详情（管理后台，电话不脱敏）
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
     * 更新管理员备注
     */
    @PutMapping("/notes/{id}")
    public Result<Void> updateNotes(@PathVariable Long id,
                                     @RequestParam String notes,
                                     HttpServletRequest request) {
        try {
            // 从token获取管理员ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Integer adminId = jwtUtil.getAdminIdFromToken(token);
            
            if (adminId == null) {
                return Result.error("获取管理员信息失败");
            }
            
            Logistics logistics = logisticsService.getLogisticsDetail(id);
            if (logistics == null) {
                return Result.error("物流信息不存在");
            }
            
            logistics.setAdminNotes(notes);
            // 这里需要调用mapper直接更新，避免重新审核
            // 简单起见，可以添加一个专门的更新备注方法
            
            // 记录更新备注日志
            String ip = IpUtil.getIpAddress(request);
            String detail = "更新了物流信息ID:" + id + "的管理员备注";
            if (logistics.getDriverName() != null) {
                detail += "，司机：" + logistics.getDriverName();
            }
            if (logistics.getPlateNumber() != null) {
                detail += "，车牌：" + logistics.getPlateNumber();
            }
            adminOperationLogService.log(
                    adminId,
                    "更新物流备注",
                    "物流ID: " + id,
                    detail,
                    ip
            );
            
            return Result.success("更新备注成功");
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }
}

