-- =============================================
-- 帮助内容表 (tb_help_content)
-- 用于存储前端"使用帮助"模块的动态内容
-- =============================================

DROP TABLE IF EXISTS `tb_help_content`;

CREATE TABLE `tb_help_content` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category` VARCHAR(50) NOT NULL COMMENT '分类：quick-start-快速入门, publish-guide-发布指南, safety-tips-安全须知, contact-us-联系客服',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容（支持HTML）',
    `sort_order` INT DEFAULT 0 COMMENT '排序号（数字越小越靠前）',
    `status` VARCHAR(20) DEFAULT 'enabled' COMMENT '状态：enabled-启用, disabled-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帮助内容表';

-- =============================================
-- 初始化数据：快速入门
-- =============================================
INSERT INTO `tb_help_content` (`category`, `title`, `content`, `sort_order`, `status`) VALUES
('quick-start', '第一步：浏览信息', '<ul><li>在首页查看最新发布的<strong>供应信息</strong>和<strong>求购信息</strong></li><li>通过顶部导航进入<strong>供应信息</strong>或<strong>求购信息</strong>页面，使用筛选功能精确查找</li><li>进入<strong>物流服务</strong>页面，查找可靠的运输资源</li></ul>', 1, 'enabled'),
('quick-start', '第二步：发布信息', '<ul><li>点击导航栏<strong>"管理信息"</strong>按钮进入管理页面</li><li>选择<strong>"发布供应信息"</strong>或<strong>"发布求购信息"</strong></li><li>填写完整的鱼苗品种、规格、数量、价格等信息</li><li>上传清晰的图片，提高信息的可信度</li><li>提交后等待管理员审核通过即可展示</li></ul>', 2, 'enabled'),
('quick-start', '第三步：联系交易', '<ul><li>找到感兴趣的信息后，点击<strong>查看电话</strong>联系对方</li><li>建议<strong>实地考察</strong>后再进行交易</li><li>签订正式合同，保留交易凭证</li><li>交易完成后可对信息进行管理</li></ul>', 3, 'enabled'),

-- 发布指南
('publish-guide', '供应信息发布', '<ul><li>详细说明鱼苗的<strong>品种、规格、数量</strong></li><li>注明<strong>价格和价格单位</strong>（如元/尾、元/万尾）</li><li>填写准确的<strong>地址信息</strong>，方便买家计算运输成本</li><li>上传<strong>清晰的实拍图片</strong></li><li>注明<strong>可发货时间和运输方式</strong></li></ul>', 1, 'enabled'),
('publish-guide', '求购信息发布', '<ul><li>明确说明需要的<strong>鱼苗品种和规格</strong></li><li>注明预期的<strong>价格区间</strong></li><li>说明<strong>交货时间和地点要求</strong></li><li>填写<strong>需求数量</strong></li></ul>', 2, 'enabled'),
('publish-guide', '物流信息发布', '<ul><li>详细填写<strong>车辆信息和载重量</strong></li><li>标明<strong>服务范围和运输路线</strong></li><li>说明<strong>特殊运输设备</strong>（冷藏、增氧等）</li><li>提供合理的<strong>运费报价</strong></li></ul>', 3, 'enabled'),

-- 安全须知
('safety-tips', '谨防低价陷阱', '<strong>价格明显低于市场价的信息需要谨慎，建议实地考察</strong>', 1, 'enabled'),
('safety-tips', '核实对方身份', '<strong>交易前请核实对方的营业执照、身份证等相关证件</strong>', 2, 'enabled'),
('safety-tips', '选择安全付款', '<strong>建议使用担保交易或货到付款，避免预付全款</strong>', 3, 'enabled'),
('safety-tips', '签订正式合同', '<strong>签订正式的买卖合同，保留交易记录和凭证</strong>', 4, 'enabled'),
('safety-tips', '优先本地交易', '<strong>优先选择本地交易，方便实地查看鱼苗质量</strong>', 5, 'enabled'),

-- 联系客服
('contact-us', '客服热线', '<strong>电话：400-xxx-xxxx</strong><br/>工作时间：周一至周五 9:00-18:00', 1, 'enabled'),
('contact-us', '电子邮箱', '<strong>邮箱：support@fishseedling.com</strong><br/>我们会在24小时内回复您的邮件', 2, 'enabled'),
('contact-us', '微信客服', '<strong>微信号：fish_seedling_kf</strong><br/>扫描二维码或搜索微信号添加客服', 3, 'enabled'),
('contact-us', '意见反馈', '<strong>欢迎提出宝贵意见！</strong><br/>请通过邮件或客服热线告诉我们您的建议和问题', 4, 'enabled');
