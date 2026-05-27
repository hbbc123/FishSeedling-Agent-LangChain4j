/*
SQLyog Ultimate v12.08 (64 bit)
MySQL - 8.0.29 : Database - fish_seedling_platform
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`fish_seedling_platform` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

/*Table structure for table `tb_admin_operation_logs` */

CREATE TABLE `tb_admin_operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id` int NOT NULL COMMENT '管理员ID',
  `operation_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型',
  `operation_object` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作对象',
  `operation_detail` text COLLATE utf8mb4_unicode_ci COMMENT '操作详情',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IP地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';

/*Table structure for table `tb_admins` */

CREATE TABLE `tb_admins` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `role` enum('super','normal') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal' COMMENT '角色：super超级管理员,normal普通管理员',
  `status` enum('normal','locked') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal' COMMENT '状态：normal正常,locked锁定',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后登录IP',
  `failed_login_count` int NOT NULL DEFAULT '0' COMMENT '登录失败次数',
  `lock_until` datetime DEFAULT NULL COMMENT '锁定截止时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

/*Table structure for table `tb_fish_breeds` */

CREATE TABLE `tb_fish_breeds` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `breed_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '品种名称',
  `breed_alias` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '别名',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0否,1是',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_breed_name` (`breed_name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鱼苗品种表';

/*Table structure for table `tb_handbooks` */

CREATE TABLE `tb_handbooks` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fish_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鱼种名称',
  `fish_alias` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '别名',
  `category` enum('freshwater','saltwater','shrimp_crab','special') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类',
  `cover_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图',
  `brief_intro` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简介',
  `difficulty` enum('easy','medium','hard') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '养殖难度',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容（富文本HTML）',
  `is_published` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否发布：0否,1是',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_is_published` (`is_published`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养殖手册表';

/*Table structure for table `tb_ip_blacklist` */

CREATE TABLE `tb_ip_blacklist` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IP地址',
  `reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '加入原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IP黑名单表';

/*Table structure for table `tb_logistics` */

CREATE TABLE `tb_logistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vehicle_type` varchar(50) NOT NULL COMMENT '车辆类型：冷藏车/水罐车/普通货车',
  `plate_number` varchar(20) NOT NULL COMMENT '车牌号码',
  `max_load` decimal(10,2) NOT NULL COMMENT '最大载重量（吨）',
  `province` varchar(50) NOT NULL COMMENT '常驻省份',
  `city` varchar(50) NOT NULL COMMENT '常驻城市',
  `district` varchar(50) DEFAULT NULL COMMENT '常驻区县',
  `driver_name` varchar(50) NOT NULL COMMENT '司机姓名',
  `contact_phone` varchar(255) NOT NULL COMMENT '联系电话（加密）',
  `price_per_km` decimal(10,2) NOT NULL COMMENT '每公里单价（元）',
  `base_price` decimal(10,2) NOT NULL COMMENT '起步价（元）',
  `base_km` decimal(10,2) DEFAULT '0.00' COMMENT '起步公里数',
  `vehicle_brand` varchar(100) DEFAULT NULL COMMENT '车辆品牌/型号',
  `vehicle_size` varchar(100) DEFAULT NULL COMMENT '车厢尺寸（长×宽×高，米）',
  `vehicle_age` int DEFAULT NULL COMMENT '车龄（年）',
  `operation_license` varchar(100) DEFAULT NULL COMMENT '营运证号',
  `suggested_load` decimal(10,2) DEFAULT NULL COMMENT '建议装载量（吨）',
  `oxygen_equipment` varchar(100) DEFAULT NULL COMMENT '制氧设备类型',
  `oxygen_count` int DEFAULT NULL COMMENT '制氧设备数量',
  `oxygen_power` varchar(50) DEFAULT NULL COMMENT '制氧设备功率',
  `has_refrigeration` tinyint(1) DEFAULT '0' COMMENT '是否有冷藏设备',
  `temp_range` varchar(50) DEFAULT NULL COMMENT '温控范围（℃）',
  `has_insulation` tinyint(1) DEFAULT '0' COMMENT '是否有保温措施',
  `has_oxygen_system` tinyint(1) DEFAULT '0' COMMENT '是否配备增氧设备',
  `has_water_circulation` tinyint(1) DEFAULT '0' COMMENT '是否有水循环系统',
  `water_tank_capacity` decimal(10,2) DEFAULT NULL COMMENT '水箱容量（立方米）',
  `service_scope` varchar(50) DEFAULT NULL COMMENT '服务范围：市内/省内/跨省',
  `service_areas` text COMMENT '可配送区域（JSON数组）',
  `accept_long_distance` tinyint(1) DEFAULT '1' COMMENT '是否接受长途运输',
  `excluded_areas` text COMMENT '不服务的区域',
  `price_unit` varchar(50) DEFAULT '元/公里/车' COMMENT '计价单位',
  `include_toll` tinyint(1) DEFAULT '0' COMMENT '是否包含过路费',
  `include_fuel` tinyint(1) DEFAULT '0' COMMENT '是否包含油费',
  `charge_empty_return` tinyint(1) DEFAULT '0' COMMENT '是否收取空返费用',
  `empty_return_rate` decimal(5,2) DEFAULT NULL COMMENT '空返费率（%）',
  `night_extra_rate` decimal(5,2) DEFAULT NULL COMMENT '夜间加价比例（%）',
  `holiday_extra_rate` decimal(5,2) DEFAULT NULL COMMENT '节假日加价比例（%）',
  `min_order_amount` decimal(10,2) DEFAULT NULL COMMENT '单次运输最低金额（元）',
  `negotiable` tinyint(1) DEFAULT '1' COMMENT '是否接受议价',
  `driving_years` int DEFAULT NULL COMMENT '驾龄（年）',
  `license_type` varchar(20) DEFAULT NULL COMMENT '驾驶证类型：A1/A2/B1/B2等',
  `work_years` int DEFAULT NULL COMMENT '从业年限（运输鱼苗经验）',
  `has_insurance` tinyint(1) DEFAULT '0' COMMENT '是否购买货物运输保险',
  `survival_rate` decimal(5,2) DEFAULT NULL COMMENT '承诺的鱼苗存活率（%）',
  `available_time` varchar(50) DEFAULT NULL COMMENT '可出车时间：随时/预约/固定时段',
  `response_time` varchar(50) DEFAULT NULL COMMENT '最快响应时间',
  `accept_urgent` tinyint(1) DEFAULT '1' COMMENT '是否接急单',
  `valid_days` int NOT NULL DEFAULT '30' COMMENT '有效天数',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `service_loading` tinyint(1) DEFAULT '0' COMMENT '是否提供装卸服务',
  `service_oxygen` tinyint(1) DEFAULT '0' COMMENT '是否协助打氧',
  `service_packaging` tinyint(1) DEFAULT '0' COMMENT '是否提供包装材料',
  `service_cod` tinyint(1) DEFAULT '0' COMMENT '是否支持代收货款',
  `service_gps` tinyint(1) DEFAULT '0' COMMENT '是否提供GPS实时跟踪',
  `payment_method` varchar(50) DEFAULT NULL COMMENT '付款方式：先付/到付/账期',
  `description` text COMMENT '详细说明',
  `special_requirements` text COMMENT '特殊要求',
  `images` json DEFAULT NULL COMMENT '车辆图片（JSON数组，最多5张）',
  `manage_code` varchar(6) NOT NULL COMMENT '管理码（6位随机码）',
  `manage_code_expire_time` datetime NOT NULL COMMENT '管理码过期时间',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending待审核,approved已通过,rejected已拒绝,expired已过期,deleted已删除',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
  `top_expire_time` datetime DEFAULT NULL COMMENT '置顶过期时间',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `phone_view_count` int DEFAULT '0' COMMENT '电话查看次数',
  `publisher_ip` varchar(50) DEFAULT NULL COMMENT '发布者IP',
  `publisher_device` varchar(500) DEFAULT NULL COMMENT '设备指纹',
  `admin_notes` varchar(500) DEFAULT NULL COMMENT '管理员备注',
  `auditor_id` int DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plate_number` (`plate_number`),
  KEY `idx_city` (`city`),
  KEY `idx_status` (`status`),
  KEY `idx_vehicle_type` (`vehicle_type`),
  KEY `idx_manage_code` (`manage_code`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物流信息表';

/*Table structure for table `tb_phone_view_logs` */

CREATE TABLE `tb_phone_view_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '信息ID',
  `viewer_ip` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '查看者IP',
  `viewer_location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地理位置',
  `view_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查看时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_viewer_ip` (`viewer_ip`),
  KEY `idx_view_time` (`view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电话查看记录表';

/*Table structure for table `tb_posts` */

CREATE TABLE `tb_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` enum('supply','purchase') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '信息类型：supply供应,purchase求购',
  `fish_breed` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鱼苗品种',
  `specification` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规格',
  `quantity` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数量',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `price_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '价格单位：元/尾、元/斤、元/公斤',
  `province` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '省份',
  `city` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '城市',
  `district` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区县',
  `detail_address` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '详细描述',
  `images` json DEFAULT NULL COMMENT '图片路径数组',
  `contact_person` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系人',
  `contact_phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话（加密）',
  `manage_code` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '管理码（6位随机码）',
  `manage_code_expire_time` datetime NOT NULL COMMENT '管理码过期时间',
  `status` enum('pending','approved','rejected','expired','deleted') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态',
  `reject_reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拒绝原因',
  `is_top` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否置顶：0否,1是',
  `top_expire_time` datetime DEFAULT NULL COMMENT '置顶过期时间',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `phone_view_count` int NOT NULL DEFAULT '0' COMMENT '电话查看次数',
  `valid_days` int NOT NULL DEFAULT '30' COMMENT '有效天数',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `publisher_ip` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发布者IP',
  `publisher_device` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备指纹',
  `admin_notes` text COLLATE utf8mb4_unicode_ci COMMENT '管理员备注',
  `auditor_id` int DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`type`,`status`),
  KEY `idx_fish_breed` (`fish_breed`),
  KEY `idx_province_city` (`province`,`city`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_manage_code` (`manage_code`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB AUTO_INCREMENT=107 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信息发布表';

/*Table structure for table `tb_regions` */

CREATE TABLE `tb_regions` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `region_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '行政区划代码',
  `region_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '地区名称',
  `parent_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '父级代码',
  `level` tinyint NOT NULL COMMENT '层级：1省,2市,3区',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_region_code` (`region_code`),
  KEY `idx_parent_code` (`parent_code`)
) ENGINE=InnoDB AUTO_INCREMENT=397 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';

/*Table structure for table `tb_reports` */

CREATE TABLE `tb_reports` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `target_type` enum('post','logistics') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '被举报对象类型：post信息,logistics物流',
  `target_id` bigint NOT NULL COMMENT '被举报对象ID',
  `reason` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报原因',
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报描述',
  `reporter_contact` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '举报人联系方式',
  `reporter_ip` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报人IP',
  `status` enum('pending','processed','ignored') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '处理状态',
  `process_result` text COLLATE utf8mb4_unicode_ci COMMENT '处理结果',
  `processor_id` int DEFAULT NULL COMMENT '处理人ID',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_target_type_id` (`target_type`,`target_id`),
  KEY `idx_reason` (`reason`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

/*Table structure for table `tb_sensitive_words` */

CREATE TABLE `tb_sensitive_words` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `word` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '敏感词',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0否,1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词表';

/*Table structure for table `tb_system_config` */

CREATE TABLE `tb_system_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `config_desc` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置说明',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
