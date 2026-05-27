package com.fishseedling.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fishseedling.platform.common.Result;
import com.fishseedling.platform.entity.FishBreed;
import com.fishseedling.platform.entity.Region;
import com.fishseedling.platform.mapper.FishBreedMapper;
import com.fishseedling.platform.mapper.RegionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 公共Controller
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/common")
public class CommonController {

    @Resource
    private FishBreedMapper fishBreedMapper;

    @Resource
    private RegionMapper regionMapper;

    @Value("${app.upload.path}")
    private String uploadPath;

    /**
     * 调试接口：检查上传配置
     */
    @GetMapping("/debug/upload-config")
    public Result<Map<String, String>> getUploadConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("uploadPath", uploadPath);
        config.put("absolutePath", new File(uploadPath).getAbsolutePath());
        config.put("exists", String.valueOf(new File(uploadPath).exists()));
        
        // 列出上传目录中的文件
        File uploadDir = new File(uploadPath);
        if (uploadDir.exists() && uploadDir.isDirectory()) {
            File[] files = uploadDir.listFiles();
            if (files != null) {
                StringBuilder fileList = new StringBuilder();
                for (File file : files) {
                    fileList.append(file.getName()).append(", ");
                }
                config.put("files", fileList.toString());
            }
        }
        
        return Result.success(config);
    }

    /**
     * 获取鱼苗品种列表
     */
    @GetMapping("/fish-breeds")
    public Result<List<FishBreed>> getFishBreeds() {
        try {
            LambdaQueryWrapper<FishBreed> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FishBreed::getIsEnabled, true)
                    .orderByAsc(FishBreed::getSortOrder);
            List<FishBreed> list = fishBreedMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取省份列表
     */
    @GetMapping("/regions/provinces")
    public Result<List<Region>> getProvinces() {
        try {
            LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Region::getLevel, 1);
            List<Region> list = regionMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据省份获取城市列表
     */
    @GetMapping("/regions/cities/{provinceCode}")
    public Result<List<Region>> getCities(@PathVariable String provinceCode) {
        try {
            LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Region::getParentCode, provinceCode)
                    .eq(Region::getLevel, 2);
            List<Region> list = regionMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据城市获取区县列表
     */
    @GetMapping("/regions/districts/{cityCode}")
    public Result<List<Region>> getDistricts(@PathVariable String cityCode) {
        try {
            LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Region::getParentCode, cityCode)
                    .eq(Region::getLevel, 3);
            List<Region> list = regionMapper.selectList(wrapper);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            // 获取文件原始名称
            String originalFilename = file.getOriginalFilename();
            // 获取文件扩展名
            String extension = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成新文件名
            String fileName = UUID.randomUUID().toString() + extension;

            // 创建上传目录（使用配置文件中的路径）
            File uploadDir = new File(uploadPath);
            System.out.println("上传目录路径: " + uploadDir.getAbsolutePath());
            
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    System.err.println("创建上传目录失败: " + uploadDir.getAbsolutePath());
                    return Result.error("创建上传目录失败");
                }
                System.out.println("成功创建上传目录: " + uploadDir.getAbsolutePath());
            }

            // 保存文件
            File dest = new File(uploadDir, fileName);
            System.out.println("保存文件路径: " + dest.getAbsolutePath());
            file.transferTo(dest);

            // 返回文件访问路径
            Map<String, String> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("url", "/api/files/" + fileName);

            System.out.println("文件上传成功: " + fileName);
            return Result.success("上传成功", result);
        } catch (IOException e) {
            System.err.println("文件上传失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}





