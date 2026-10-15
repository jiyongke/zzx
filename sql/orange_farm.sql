-- ============================================================
-- 橙心助农农产品溯源电商平台 数据库初始化脚本
-- 适用 MySQL 5.6+ / 8.x，字符集 utf8mb4
-- 导入方式: mysql -uroot -p < orange_farm.sql
-- ============================================================

DROP DATABASE IF EXISTS orange_farm;
CREATE DATABASE orange_farm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE orange_farm;

-- ------------------------------------------------------------
-- 1. 用户表（角色: ADMIN 管理员 / FARMER 农户 / CONSUMER 消费者；不设独立物流角色）
--    农户需完成实名与资质认证（audit_status）并经管理员审核后方可发布溯源链/上架商品
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`         VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password`         VARCHAR(100) NOT NULL COMMENT '密码(MD5加密)',
  `nickname`         VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `phone`            VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `avatar`           VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role`             VARCHAR(20)  NOT NULL DEFAULT 'CONSUMER' COMMENT '角色: ADMIN/FARMER/CONSUMER',
  `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
  `real_name`        VARCHAR(50)  DEFAULT NULL COMMENT '农户实名',
  `id_card`          VARCHAR(30)  DEFAULT NULL COMMENT '农户身份证号',
  `qualification`    VARCHAR(1000) DEFAULT NULL COMMENT '资质证照/营业执照(URL)',
  `quality_material` VARCHAR(1000) DEFAULT NULL COMMENT '质量安全材料(检测报告/产地证明,URL逗号分隔)',
  `audit_status`     TINYINT      DEFAULT NULL COMMENT '资质审核状态: 0未提交 1待审核 2已通过 3已驳回(仅农户)',
  `audit_remark`     VARCHAR(255) DEFAULT NULL COMMENT '资质审核意见(驳回原因)',
  `audit_time`       DATETIME     DEFAULT NULL COMMENT '资质审核时间',
  `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 商品分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`   VARCHAR(50) NOT NULL COMMENT '分类名称',
  `icon`   VARCHAR(255) DEFAULT NULL COMMENT '图标',
  `sort`   INT         NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT     NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ------------------------------------------------------------
-- 3. 溯源批次表（核心：一条批次对应一类农产品）
--    status: 0待完善 1可溯源 2已下架 3待审核
--    hash_value: 发布时对溯源链计算 SHA-256 摘要哈希存证，查询时重算比对（存证可信）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `trace_batch`;
CREATE TABLE `trace_batch` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `batch_code`     VARCHAR(32)  NOT NULL COMMENT '溯源批次号(二维码内容)',
  `product_name`   VARCHAR(100) NOT NULL COMMENT '农产品名称',
  `category_id`    BIGINT       DEFAULT NULL COMMENT '所属分类ID',
  `origin`         VARCHAR(200) DEFAULT NULL COMMENT '产地',
  `farmer_id`      BIGINT       NOT NULL COMMENT '生产主体(农户)ID',
  `farmer_name`    VARCHAR(50)  DEFAULT NULL COMMENT '生产主体名称',
  `plant_date`     DATE         DEFAULT NULL COMMENT '种植日期',
  `harvest_date`   DATE         DEFAULT NULL COMMENT '采收日期',
  `description`    TEXT         COMMENT '批次说明',
  `cover_image`    VARCHAR(255) DEFAULT NULL COMMENT '批次封面图',
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0待完善 1可溯源 2已下架 3待审核',
  `hash_value`     VARCHAR(64)  DEFAULT NULL COMMENT '溯源链摘要哈希(SHA-256)',
  `scan_count`     INT          NOT NULL DEFAULT 0 COMMENT '累计扫码查询次数',
  `last_scan_time` DATETIME     DEFAULT NULL COMMENT '最近扫码查询时间',
  `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_code` (`batch_code`),
  KEY `idx_farmer` (`farmer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='溯源批次表';

-- ------------------------------------------------------------
-- 4. 溯源环节记录表（八大环节：选种/施肥/防治/采收/加工/质检/仓储/物流）
--    发布后记录锁定，仅管理员可追加更正记录(CORRECTION)
--    recorder_* 为录入人留痕字段（存证可信）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `trace_node`;
CREATE TABLE `trace_node` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '环节ID',
  `batch_id`      BIGINT       NOT NULL COMMENT '溯源批次ID',
  `node_type`     VARCHAR(20)  NOT NULL COMMENT '环节类型: PLANT选种/FERTILIZE施肥/PEST防治/HARVEST采收/PROCESS加工/QUALITY质检/STORAGE仓储/LOGISTICS物流/CORRECTION更正',
  `title`         VARCHAR(100) NOT NULL COMMENT '环节标题',
  `description`   TEXT         COMMENT '环节描述',
  `images`        VARCHAR(1000) DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `operator`      VARCHAR(50)  DEFAULT NULL COMMENT '操作人/机构',
  `location`      VARCHAR(200) DEFAULT NULL COMMENT '地点',
  `record_time`   DATETIME     DEFAULT NULL COMMENT '环节时间',
  `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序(小在前)',
  `recorder_id`   BIGINT       DEFAULT NULL COMMENT '录入人ID',
  `recorder_role` VARCHAR(20)  DEFAULT NULL COMMENT '录入人角色',
  `recorder_time` DATETIME     DEFAULT NULL COMMENT '录入时间',
  PRIMARY KEY (`id`),
  KEY `idx_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='溯源环节表';

-- ------------------------------------------------------------
-- 5. 商品表（商品可关联溯源批次）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name`        VARCHAR(100)  NOT NULL COMMENT '商品名称',
  `category_id` BIGINT        DEFAULT NULL COMMENT '分类ID',
  `batch_id`    BIGINT        DEFAULT NULL COMMENT '关联溯源批次ID',
  `farmer_id`   BIGINT        DEFAULT NULL COMMENT '生产主体(农户)ID，平台自营为空',
  `farmer_name` VARCHAR(50)   DEFAULT NULL COMMENT '生产主体(农户)名称',
  `price`       DECIMAL(10,2) NOT NULL COMMENT '价格(元)',
  `stock`       INT           NOT NULL DEFAULT 0 COMMENT '库存',
  `sales`       INT           NOT NULL DEFAULT 0 COMMENT '销量',
  `cover_image` VARCHAR(255)  DEFAULT NULL COMMENT '封面图',
  `images`      VARCHAR(1000) DEFAULT NULL COMMENT '轮播图(逗号分隔)',
  `description` TEXT          COMMENT '商品详情',
  `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 1上架 0下架 3待审核',
  `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_farmer` (`farmer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ------------------------------------------------------------
-- 6. 收货地址表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
  `receiver`   VARCHAR(50)  NOT NULL COMMENT '收货人',
  `phone`      VARCHAR(20)  NOT NULL COMMENT '联系电话',
  `province`   VARCHAR(50)  DEFAULT NULL COMMENT '省',
  `city`       VARCHAR(50)  DEFAULT NULL COMMENT '市',
  `district`   VARCHAR(50)  DEFAULT NULL COMMENT '区县',
  `detail`     VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
  `is_default` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认: 1是 0否',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ------------------------------------------------------------
-- 7. 购物车表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `product_id`  BIGINT   NOT NULL COMMENT '商品ID',
  `quantity`    INT      NOT NULL DEFAULT 1 COMMENT '数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ------------------------------------------------------------
-- 8. 订单表（物流履约作为订单状态机的一环，不设独立物流角色）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`         VARCHAR(32)   NOT NULL COMMENT '订单编号',
  `user_id`          BIGINT        NOT NULL COMMENT '下单用户ID',
  `total_amount`     DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `status`           VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING待支付/PAID已支付/SHIPPED已发货/COMPLETED已完成/CANCELLED已取消/REFUNDING售后中/REFUNDED已退款',
  `receiver`         VARCHAR(50)   DEFAULT NULL COMMENT '收货人',
  `phone`            VARCHAR(20)   DEFAULT NULL COMMENT '收货电话',
  `address`          VARCHAR(300)  DEFAULT NULL COMMENT '收货地址(冗余)',
  `remark`           VARCHAR(200)  DEFAULT NULL COMMENT '订单备注',
  `refund_reason`    VARCHAR(255)  DEFAULT NULL COMMENT '售后申请原因',
  `refund_from`      VARCHAR(20)   DEFAULT NULL COMMENT '售后来源状态(PAID/SHIPPED)',
  `courier_company`  VARCHAR(50)   DEFAULT NULL COMMENT '快递公司(发货时填写)',
  `tracking_no`      VARCHAR(50)   DEFAULT NULL COMMENT '运单号(发货时填写)',
  `delivery_status`  VARCHAR(20)   DEFAULT NULL COMMENT '配送状态: SHIPPED已发货/DELIVERING配送中/SIGNED已签收',
  `pay_time`         DATETIME      DEFAULT NULL COMMENT '支付时间',
  `ship_time`        DATETIME      DEFAULT NULL COMMENT '发货时间',
  `complete_time`    DATETIME      DEFAULT NULL COMMENT '完成时间',
  `refund_time`      DATETIME      DEFAULT NULL COMMENT '退款时间',
  `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ------------------------------------------------------------
-- 9. 订单明细表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id`      BIGINT        NOT NULL COMMENT '订单ID',
  `product_id`    BIGINT        DEFAULT NULL COMMENT '商品ID',
  `product_name`  VARCHAR(100)  DEFAULT NULL COMMENT '商品名称(快照)',
  `product_image` VARCHAR(255)  DEFAULT NULL COMMENT '商品图片(快照)',
  `price`         DECIMAL(10,2) DEFAULT NULL COMMENT '成交单价',
  `quantity`      INT           DEFAULT NULL COMMENT '数量',
  `amount`        DECIMAL(10,2) DEFAULT NULL COMMENT '小计金额',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ------------------------------------------------------------
-- 10. 物流跟踪记录表（订单物流时间轴节点）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `logistics_log`;
CREATE TABLE `logistics_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
  `node_time`   DATETIME     NOT NULL COMMENT '节点时间',
  `title`       VARCHAR(50)  NOT NULL COMMENT '节点标题(已发货/配送中/已签收)',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '节点说明(快递公司/运单号等)',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流跟踪记录表';

-- ------------------------------------------------------------
-- 11. 操作审计日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`     BIGINT       DEFAULT NULL COMMENT '操作用户ID',
  `username`    VARCHAR(50)  DEFAULT NULL COMMENT '操作用户名',
  `role`        VARCHAR(20)  DEFAULT NULL COMMENT '操作角色',
  `action`      VARCHAR(50)  NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/PUBLISH/OFFLINE/SHIP/QUALIFICATION_APPROVE等',
  `target_type` VARCHAR(50)  DEFAULT NULL COMMENT '对象类型: PRODUCT/CATEGORY/USER/ORDER/TRACE_BATCH/TRACE_NODE',
  `target_id`   BIGINT       DEFAULT NULL COMMENT '对象ID',
  `detail`      VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- ============================================================
-- 初始数据（密码均为 123456，MD5: e10adc3949ba59abbe56e057f20f883e）
-- ============================================================
INSERT INTO `user` (`id`,`username`,`password`,`nickname`,`phone`,`avatar`,`role`,`status`,`real_name`,`id_card`,`qualification`,`quality_material`,`audit_status`,`audit_remark`,`audit_time`) VALUES
(1,'admin','e10adc3949ba59abbe56e057f20f883e','平台管理员','13800000001',NULL,'ADMIN',1,NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(2,'farmer01','e10adc3949ba59abbe56e057f20f883e','赣南脐橙合作社','13800000002',NULL,'FARMER',1,'刘德贵','360722199203154218','https://picsum.photos/seed/license1/800/600','https://picsum.photos/seed/report1/800/600,https://picsum.photos/seed/origin1/800/600',2,'', '2026-09-20 10:00:00'),
(3,'farmer02','e10adc3949ba59abbe56e057f20f883e','烟台苹果种植基地','13800000003',NULL,'FARMER',1,'王志远','370686198804123627','https://picsum.photos/seed/license2/800/600','https://picsum.photos/seed/report2/800/600,https://picsum.photos/seed/origin2/800/600',2,'', '2026-09-21 10:00:00'),
(4,'farmer03','e10adc3949ba59abbe56e057f20f883e','五常稻花香农场','13800000004',NULL,'FARMER',1,'赵明山','230184197905063318','https://picsum.photos/seed/license3/800/600','https://picsum.photos/seed/report3/800/600,https://picsum.photos/seed/origin3/800/600',2,'', '2026-09-22 10:00:00'),
(5,'consumer01','e10adc3949ba59abbe56e057f20f883e','橙心用户小王','13800000006',NULL,'CONSUMER',1,NULL,NULL,NULL,NULL,NULL,NULL,NULL);

INSERT INTO `category` (`id`,`name`,`icon`,`sort`,`status`) VALUES
(1,'时令水果','🍊',1,1),
(2,'有机蔬菜','🥬',2,1),
(3,'五谷杂粮','🌾',3,1),
(4,'禽蛋肉品','🥚',4,1),
(5,'干货特产','🌰',5,1);

INSERT INTO `trace_batch` (`id`,`batch_code`,`product_name`,`category_id`,`origin`,`farmer_id`,`farmer_name`,`plant_date`,`harvest_date`,`description`,`cover_image`,`status`) VALUES
(1,'TR20260901001','赣南脐橙',1,'江西省赣州市信丰县安西镇',2,'赣南脐橙合作社','2026-03-15','2026-09-20','选用赣南红壤丘陵种植的纽荷尔脐橙，山泉水灌溉，施用农家有机肥，全程可溯源。','http://localhost:8080/images/orange.jpg',1),
(2,'TR20260901002','烟台红富士苹果',1,'山东省烟台市栖霞市',3,'烟台苹果种植基地','2026-04-10','2026-09-25','栖霞红富士，套袋栽培，昼夜温差大，糖心率高，果肉脆甜多汁。','http://localhost:8080/images/apple.jpg',1),
(3,'TR20260901003','有机菠菜',2,'山东省寿光市蔬菜基地',2,'赣南脐橙合作社','2026-08-01','2026-09-12','无土栽培有机菠菜，全程不使用化学农药，欧盟有机认证标准种植。','http://localhost:8080/images/spinach.jpg',1),
(4,'TR20260901004','五常稻花香大米',3,'黑龙江省哈尔滨市五常市',4,'五常稻花香农场','2026-05-20','2026-09-28','五常核心产区稻花香2号，拉林河活水灌溉，一年一季，米粒饱满，饭香浓郁。','http://localhost:8080/images/rice.jpg',1),
(5,'TR20260905001','果园散养土鸡蛋',4,'江西省赣州市信丰县',2,'赣南脐橙合作社','2026-03-01','2026-09-25','赣南丘陵果园生态放养，谷物喂养，每日拾蛋，逐枚光照检测，全程可溯源。','http://localhost:8080/images/egg.jpg',1),
(6,'TR20260905002','散养土鸡',4,'江西省赣州市信丰县',2,'赣南脐橙合作社','2026-04-01','2026-09-26','果园散养180天足龄出栏，五谷喂养，检疫合格，冷链直达。','http://localhost:8080/images/chicken.jpg',1),
(7,'TR20260905003','赣南白莲子',5,'江西省赣州市石城县',2,'赣南脐橙合作社','2026-04-20','2026-09-18','赣南荷塘传统品种，山泉水灌溉，手工去芯自然晾晒，颗粒饱满。','http://localhost:8080/images/lotus.jpg',1),
(8,'TR20260905004','东北黑木耳',5,'黑龙江省尚志市',4,'五常稻花香农场','2026-05-10','2026-09-15','长白山椴木栽培，山泉浇灌，自然晾晒不硫熏，泡发率高。','http://localhost:8080/images/fungus.jpg',1),
(9,'TR20260905005','有机西兰花',2,'山东省寿光市蔬菜基地',2,'赣南脐橙合作社','2026-08-15','2026-09-27','有机栽培，物理防虫，清晨采收，农残速检合格后冷链直发。','http://localhost:8080/images/broccoli.jpg',1),
(10,'TR20260905006','五常黑米',3,'黑龙江省哈尔滨市五常市',4,'五常稻花香农场','2026-05-20','2026-09-29','五常核心产区黑稻品种，霜前收割，低温烘干保留花青素，真空包装。','http://localhost:8080/images/blackrice.jpg',1);

-- 溯源环节（每个批次按八大环节业务顺序 PLANT→FERTILIZE→PEST→HARVEST→PROCESS→QUALITY→STORAGE→LOGISTICS 各一条）
INSERT INTO `trace_node` (`batch_id`,`node_type`,`title`,`description`,`images`,`operator`,`location`,`record_time`,`sort`,`recorder_id`,`recorder_role`,`recorder_time`) VALUES
-- 批次1 赣南脐橙
(1,'PLANT','选种育苗','选用纽荷尔脐橙脱毒苗，基地大棚集中育苗，苗龄8个月以上。','https://picsum.photos/seed/nursery1/800/600','赣南脐橙合作社','江西信丰安西镇育苗基地','2026-03-15 08:30:00',1,2,'FARMER','2026-09-22 09:00:00'),
(1,'FERTILIZE','有机施肥','每亩施用腐熟农家肥2.5吨，配合生物菌肥改良土壤，不使用化学激素。','https://picsum.photos/seed/fertilize1/800/600','赣南脐橙合作社','江西信丰安西镇果园','2026-04-02 09:00:00',2,2,'FARMER','2026-09-22 09:05:00'),
(1,'PEST','生物防治','采用黄板、性诱剂等绿色防控技术，全程零高毒农药，农残检测合格。','https://picsum.photos/seed/pest1/800/600','赣南脐橙合作社','江西信丰安西镇果园','2026-06-20 10:00:00',3,2,'FARMER','2026-09-22 09:10:00'),
(1,'HARVEST','人工采收','糖度达到12°以上后人工采摘，果柄保留完整，当日运输入库。','https://picsum.photos/seed/harvest1/800/600','赣南脐橙合作社','江西信丰安西镇果园','2026-09-20 07:00:00',4,2,'FARMER','2026-09-22 09:15:00'),
(1,'PROCESS','分级分拣','按果径60-85mm分级，人工挑选病果、畸形果，清水冲洗后打蜡保鲜。','https://picsum.photos/seed/process1/800/600','橙心助农分拣中心','江西信丰加工厂','2026-09-21 14:00:00',5,2,'FARMER','2026-09-22 09:20:00'),
(1,'QUALITY','质检认证','第三方机构抽检：糖度12.8°，农残检测全部未检出，出具质检合格报告。','https://picsum.photos/seed/quality1/800/600','橙心助农质检中心','江西信丰检测实验室','2026-09-22 10:00:00',6,1,'ADMIN','2026-09-22 11:00:00'),
(1,'STORAGE','冷库保鲜','分选后入冷库 4-6℃ 预冷保鲜，库内温度实时监控记录。','https://picsum.photos/seed/storage1/800/600','橙心助农仓储中心','江西信丰冷库','2026-09-22 15:00:00',7,2,'FARMER','2026-09-22 15:30:00'),
(1,'LOGISTICS','冷链发货','全程冷链车运输，恒温8-10℃，48小时内送达，附运输轨迹单。','https://picsum.photos/seed/logistics1/800/600','顺源冷链物流','信丰-全国冷链干线','2026-09-23 06:00:00',8,2,'FARMER','2026-09-23 06:30:00'),
-- 批次2 烟台红富士苹果
(2,'PLANT','果园定植','栖霞山前平原沙壤土，株行距4×5米，授粉树按比例配置。','https://picsum.photos/seed/nursery2/800/600','烟台苹果种植基地','山东烟台栖霞市','2026-04-10 08:00:00',1,3,'FARMER','2026-09-23 09:00:00'),
(2,'FERTILIZE','水肥一体','滴灌水肥一体化，以有机肥为主，果实膨大期增施钾肥提升糖度。','https://picsum.photos/seed/fertilize2/800/600','烟台苹果种植基地','山东烟台栖霞市','2026-06-15 09:00:00',2,3,'FARMER','2026-09-23 09:05:00'),
(2,'PEST','套袋防虫','幼果期全园套袋，物理隔离病虫害，减少农药使用量80%以上。','https://picsum.photos/seed/pest2/800/600','烟台苹果种植基地','山东烟台栖霞市','2026-07-01 10:00:00',3,3,'FARMER','2026-09-23 09:10:00'),
(2,'HARVEST','适期采收','霜降后分批采收，糖度平均14.5%，现摘现装，不存冷库压货。','https://picsum.photos/seed/harvest2/800/600','烟台苹果种植基地','山东烟台栖霞市','2026-09-25 07:00:00',4,3,'FARMER','2026-09-25 08:00:00'),
(2,'PROCESS','分级包装','按果径70-85mm分级，人工挑选套网袋装箱，贴溯源标贴。','https://picsum.photos/seed/process2/800/600','烟台苹果种植基地','山东烟台分拣车间','2026-09-25 14:00:00',5,3,'FARMER','2026-09-25 15:00:00'),
(2,'QUALITY','农残检测','SGS认证实验室检测，48项农残指标全部未检出，符合出口标准。','https://picsum.photos/seed/quality2/800/600','橙心助农质检中心','烟台检测实验室','2026-09-26 11:00:00',6,1,'ADMIN','2026-09-26 12:00:00'),
(2,'STORAGE','气调冷藏','0-2℃气调库冷藏锁鲜，延缓果实衰老，保持脆度与糖度。','https://picsum.photos/seed/storage2/800/600','橙心助农仓储中心','烟台冷库','2026-09-26 16:00:00',7,3,'FARMER','2026-09-26 16:30:00'),
(2,'LOGISTICS','冷链直发','泡沫箱+冰袋包装，顺丰冷链专线，72小时宅配到家。','https://picsum.photos/seed/logistics2/800/600','顺源冷链物流','烟台-全国干线','2026-09-27 09:00:00',8,3,'FARMER','2026-09-27 09:30:00'),
-- 批次3 有机菠菜
(3,'PLANT','无土栽培','椰糠基质无土栽培，自动化水肥循环系统，杜绝土传病害。','https://picsum.photos/seed/nursery3/800/600','寿光蔬菜基地','山东寿光现代农业产业园','2026-08-01 08:00:00',1,2,'FARMER','2026-09-10 09:00:00'),
(3,'FERTILIZE','营养液配方','按菠菜生长周期调配营养液EC值1.8-2.2，全程零化学农药。','https://picsum.photos/seed/fertilize3/800/600','寿光蔬菜基地','山东寿光现代农业产业园','2026-08-10 09:00:00',2,2,'FARMER','2026-09-10 09:05:00'),
(3,'PEST','物理防虫','防虫网+黄蓝粘虫板物理防控，全程无化学农药残留。','https://picsum.photos/seed/pest3/800/600','寿光蔬菜基地','山东寿光现代农业产业园','2026-08-25 10:00:00',3,2,'FARMER','2026-09-10 09:10:00'),
(3,'HARVEST','清晨采收','凌晨4点采收锁鲜，叶面含水率高，全程冷链短链配送。','https://picsum.photos/seed/harvest3/800/600','寿光蔬菜基地','山东寿光现代农业产业园','2026-09-12 04:30:00',4,2,'FARMER','2026-09-12 05:00:00'),
(3,'PROCESS','清洗分拣','采收后2小时内清洗分拣，去除黄叶老叶，按标准捆扎装袋。','https://picsum.photos/seed/process3/800/600','寿光蔬菜基地','山东寿光分拣车间','2026-09-12 05:30:00',5,2,'FARMER','2026-09-12 06:00:00'),
(3,'QUALITY','农残速检','采收后1小时内完成农残快速检测，抑制率<40%，合格出厂。','https://picsum.photos/seed/quality3/800/600','寿光蔬菜基地','山东寿光检测站','2026-09-12 06:00:00',6,1,'ADMIN','2026-09-12 06:30:00'),
(3,'STORAGE','冷藏暂存','4℃冷库暂存周转，全程温控记录，保障新鲜度。','https://picsum.photos/seed/storage3/800/600','橙心助农仓储中心','寿光冷库','2026-09-12 06:30:00',7,2,'FARMER','2026-09-12 06:40:00'),
(3,'LOGISTICS','冷藏短链','4℃冷藏车同城/省内直配，从采收到上架不超过24小时。','https://picsum.photos/seed/logistics3/800/600','顺源冷链物流','寿光-山东全省','2026-09-12 07:00:00',8,2,'FARMER','2026-09-12 07:10:00'),
-- 批次4 五常稻花香大米
(4,'PLANT','育秧插秧','五常稻花香2号品种，4月中育秧，5月下旬人工插秧，稀植通风。','https://picsum.photos/seed/nursery4/800/600','五常稻花香农场','黑龙江五常市核心产区','2026-05-20 08:00:00',1,4,'FARMER','2026-09-25 09:00:00'),
(4,'FERTILIZE','有机肥田','秋季深耕施入腐熟有机肥，配合秸秆还田，土壤有机质含量3.2%。','https://picsum.photos/seed/fertilize4/800/600','五常稻花香农场','黑龙江五常市核心产区','2026-06-10 09:00:00',2,4,'FARMER','2026-09-25 09:05:00'),
(4,'PEST','绿色防控','稻田养鸭除草除虫，物理杀虫灯+生物农药，无化学残留。','https://picsum.photos/seed/pest4/800/600','五常稻花香农场','黑龙江五常市核心产区','2026-07-15 10:00:00',3,4,'FARMER','2026-09-25 09:10:00'),
(4,'HARVEST','霜前收割','10月初霜前收割，自然晾晒15天后低温烘干，保留胚芽营养。','https://picsum.photos/seed/harvest4/800/600','五常稻花香农场','黑龙江五常市核心产区','2026-09-28 08:00:00',4,4,'FARMER','2026-09-28 09:00:00'),
(4,'PROCESS','恒温碾米','恒温车间碾米，轻度抛光保留米糠营养，现磨现发不掺陈米。','https://picsum.photos/seed/process4/800/600','五常稻花香农场','黑龙江五常加工车间','2026-09-29 10:00:00',5,4,'FARMER','2026-09-29 11:00:00'),
(4,'QUALITY','批次质检','每批次检测水分≤14.5%、碎米率≤5%，出具产地检测报告。','https://picsum.photos/seed/quality4/800/600','橙心助农质检中心','五常检测实验室','2026-09-30 09:00:00',6,1,'ADMIN','2026-09-30 10:00:00'),
(4,'STORAGE','低温仓储','15℃以下低温粮仓储存，湿度60%以下，防潮防虫防霉变。','https://picsum.photos/seed/storage4/800/600','五常稻花香农场','五常低温粮仓','2026-09-30 11:00:00',7,4,'FARMER','2026-09-30 11:30:00'),
(4,'LOGISTICS','真空包装','5kg真空包装防潮保鲜，普通快递全国包邮，3-5天送达。','https://picsum.photos/seed/logistics4/800/600','顺源冷链物流','五常-全国干线','2026-09-30 14:00:00',8,4,'FARMER','2026-09-30 14:30:00'),
-- 批次5 果园散养土鸡蛋
(5,'PLANT','果园放养','赣南丘陵果园自然放养，日间自由采食，活动空间充足。','https://picsum.photos/seed/nursery5/800/600','赣南脐橙合作社','江西赣南信丰果园','2026-03-01 08:00:00',1,2,'FARMER','2026-09-23 09:00:00'),
(5,'FERTILIZE','谷物喂养','以玉米、稻谷为主粮，辅以菜叶青饲料，全程不添加激素。','https://picsum.photos/seed/fertilize5/800/600','赣南脐橙合作社','江西赣南信丰果园','2026-03-20 09:00:00',2,2,'FARMER','2026-09-23 09:05:00'),
(5,'PEST','生态防疫','定期中草药防疫+规范疫苗接种，全周期不使用抗生素。','https://picsum.photos/seed/pest5/800/600','赣南脐橙合作社','江西赣南信丰果园','2026-05-10 10:00:00',3,2,'FARMER','2026-09-23 09:10:00'),
(5,'HARVEST','每日拾蛋','每日清晨人工拾蛋，蛋壳洁净完整，当日完成分拣入库。','https://picsum.photos/seed/harvest5/800/600','赣南脐橙合作社','江西赣南信丰果园','2026-09-25 06:30:00',4,2,'FARMER','2026-09-25 07:00:00'),
(5,'PROCESS','清洗分级','45℃温水清洗烘干，按克重分档，剔除破损散黄蛋。','https://picsum.photos/seed/process5/800/600','橙心助农分拣中心','江西赣南分拣中心','2026-09-25 08:00:00',5,2,'FARMER','2026-09-25 08:30:00'),
(5,'QUALITY','光照检测','逐枚光照检测，剔除裂纹蛋、散黄蛋，按克重分级装托。','https://picsum.photos/seed/quality5/800/600','橙心助农质检中心','江西赣南分拣中心','2026-09-25 09:00:00',6,1,'ADMIN','2026-09-25 10:00:00'),
(5,'STORAGE','恒温保鲜','10-15℃恒温库短期存放，定期翻蛋，保证蛋品新鲜。','https://picsum.photos/seed/storage5/800/600','橙心助农仓储中心','江西赣南恒温库','2026-09-25 11:00:00',7,2,'FARMER','2026-09-25 11:30:00'),
(5,'LOGISTICS','珍珠棉运输','珍珠棉蛋托防震包装，快递48小时配送到家，破损包赔。','https://picsum.photos/seed/logistics5/800/600','顺源冷链物流','赣州-全国干线','2026-09-26 07:00:00',8,2,'FARMER','2026-09-26 07:30:00'),
-- 批次6 散养土鸡
(6,'PLANT','果园散养','果园生态放养180天，运动量大，肉质紧实弹嫩。','https://picsum.photos/seed/nursery6/800/600','赣南脐橙合作社','江西赣南信丰果园','2026-04-01 08:00:00',1,2,'FARMER','2026-09-24 09:00:00'),
(6,'FERTILIZE','五谷喂养','玉米、稻谷、麸皮科学配比喂养，不催肥、不注水。','https://picsum.photos/seed/fertilize6/800/600','赣南脐橙合作社','江西赣南信丰果园','2026-04-20 09:00:00',2,2,'FARMER','2026-09-24 09:05:00'),
(6,'PEST','生态防疫','中草药保健+规范免疫程序，全周期无抗生素残留。','https://picsum.photos/seed/pest6/800/600','赣南脐橙合作社','江西赣南信丰果园','2026-06-01 10:00:00',3,2,'FARMER','2026-09-24 09:10:00'),
(6,'HARVEST','足龄出栏','180天足龄出栏，凌晨宰杀，立即冷鲜处理锁鲜。','https://picsum.photos/seed/harvest6/800/600','赣南脐橙合作社','江西赣南宰杀车间','2026-09-26 04:00:00',4,2,'FARMER','2026-09-26 05:00:00'),
(6,'PROCESS','净膛宰杀','人工净膛去杂，0-4℃冷鲜排酸12小时，真空贴体包装。','https://picsum.photos/seed/process6/800/600','橙心助农加工中心','江西赣南加工车间','2026-09-26 09:00:00',5,2,'FARMER','2026-09-26 09:30:00'),
(6,'QUALITY','检疫合格','出具动物检疫合格证明，耳标溯源信息全程可查。','https://picsum.photos/seed/quality6/800/600','橙心助农质检中心','江西赣南检疫站','2026-09-26 11:00:00',6,1,'ADMIN','2026-09-26 11:30:00'),
(6,'STORAGE','冷鲜暂存','0-4℃冷鲜库暂存，温控记录完整，出库前二次质检。','https://picsum.photos/seed/storage6/800/600','橙心助农仓储中心','江西赣南冷鲜库','2026-09-26 12:00:00',7,2,'FARMER','2026-09-26 12:30:00'),
(6,'LOGISTICS','冷链直达','冰袋+保温箱冷链运输，48小时宅配到家。','https://picsum.photos/seed/logistics6/800/600','顺源冷链物流','赣州-全国冷链','2026-09-27 06:00:00',8,2,'FARMER','2026-09-27 06:30:00'),
-- 批次7 赣南白莲子
(7,'PLANT','白莲种植','赣南荷塘传统品种，山泉水灌溉，清明前后种藕。','https://picsum.photos/seed/nursery7/800/600','赣南脐橙合作社','江西赣南石城荷塘','2026-04-20 08:00:00',1,2,'FARMER','2026-09-22 09:00:00'),
(7,'FERTILIZE','有机基肥','塘泥+腐熟农家肥作基肥，全生育期不施化肥。','https://picsum.photos/seed/fertilize7/800/600','赣南脐橙合作社','江西赣南石城荷塘','2026-05-10 09:00:00',2,2,'FARMER','2026-09-22 09:05:00'),
(7,'PEST','绿色防控','生物天敌+人工除虫，莲子生长全程零农残。','https://picsum.photos/seed/pest7/800/600','赣南脐橙合作社','江西赣南石城荷塘','2026-06-20 10:00:00',3,2,'FARMER','2026-09-22 09:10:00'),
(7,'HARVEST','人工采摘','莲蓬成熟后人工采摘，当日去壳取莲，新鲜加工。','https://picsum.photos/seed/harvest7/800/600','赣南脐橙合作社','江西赣南石城荷塘','2026-09-18 07:00:00',4,2,'FARMER','2026-09-18 08:00:00'),
(7,'PROCESS','手工去芯','手工剥莲子、通芯，自然晾晒至含水率≤12%，粒粒完整。','https://picsum.photos/seed/process7/800/600','赣南脐橙合作社','江西赣南加工坊','2026-09-19 09:00:00',5,2,'FARMER','2026-09-19 10:00:00'),
(7,'QUALITY','品质检测','颗粒饱满均匀，检测黄曲霉毒素未检出，出厂合格。','https://picsum.photos/seed/quality7/800/600','橙心助农质检中心','江西赣南检测室','2026-09-20 10:00:00',6,1,'ADMIN','2026-09-20 11:00:00'),
(7,'STORAGE','干燥储存','阴凉干燥库储存，含水率严格控制在12%以下，防潮防虫。','https://picsum.photos/seed/storage7/800/600','橙心助农仓储中心','江西赣南干库','2026-09-20 15:00:00',7,2,'FARMER','2026-09-20 15:30:00'),
(7,'LOGISTICS','密封防潮','食品级密封袋+干燥剂防潮，避光储存，保质期12个月。','https://picsum.photos/seed/logistics7/800/600','顺源冷链物流','赣州-全国干线','2026-09-21 08:00:00',8,2,'FARMER','2026-09-21 08:30:00'),
-- 批次8 东北黑木耳
(8,'PLANT','椴木栽培','长白山椴木段栽培，模拟野生生长环境，无袋料栽培。','https://picsum.photos/seed/nursery8/800/600','五常稻花香农场','黑龙江尚志椴木基地','2026-05-10 08:00:00',1,4,'FARMER','2026-09-24 09:00:00'),
(8,'FERTILIZE','山泉浇灌','天然山泉水浇灌，不施用任何人工合成肥料。','https://picsum.photos/seed/fertilize8/800/600','五常稻花香农场','黑龙江尚志椴木基地','2026-06-10 09:00:00',2,4,'FARMER','2026-09-24 09:05:00'),
(8,'PEST','自然防控','全程零农药，依托自然生态环境防病防虫。','https://picsum.photos/seed/pest8/800/600','五常稻花香农场','黑龙江尚志椴木基地','2026-07-15 10:00:00',3,4,'FARMER','2026-09-24 09:10:00'),
(8,'HARVEST','人工采摘','耳片充分舒展后人工采摘，朵形完整，无根无杂质。','https://picsum.photos/seed/harvest8/800/600','五常稻花香农场','黑龙江尚志椴木基地','2026-09-15 08:00:00',4,4,'FARMER','2026-09-15 09:00:00'),
(8,'PROCESS','自然晾晒','东北干爽气候自然晾晒，不硫熏、不染色，保留本味。','https://picsum.photos/seed/process8/800/600','五常稻花香农场','黑龙江尚志晾晒场','2026-09-16 10:00:00',5,4,'FARMER','2026-09-16 11:00:00'),
(8,'QUALITY','杂质检测','泡发率≥10倍，检测无二氧化硫残留，肉质肥厚。','https://picsum.photos/seed/quality8/800/600','橙心助农质检中心','黑龙江尚志检测室','2026-09-17 09:00:00',6,1,'ADMIN','2026-09-17 10:00:00'),
(8,'STORAGE','干燥仓储','低温干燥库储存，湿度50%以下，防止回潮霉变。','https://picsum.photos/seed/storage8/800/600','五常稻花香农场','尚志干燥库','2026-09-17 14:00:00',7,4,'FARMER','2026-09-17 14:30:00'),
(8,'LOGISTICS','密封包装','食品级密封袋包装，防潮防虫，保质期18个月。','https://picsum.photos/seed/logistics8/800/600','顺源冷链物流','哈尔滨-全国干线','2026-09-18 08:00:00',8,4,'FARMER','2026-09-18 08:30:00'),
-- 批次9 有机西兰花
(9,'PLANT','育苗移栽','穴盘育苗25天，壮苗移栽，合理密植，通风透光。','https://picsum.photos/seed/nursery9/800/600','寿光蔬菜基地','山东寿光现代农业产业园','2026-08-15 08:00:00',1,2,'FARMER','2026-09-25 09:00:00'),
(9,'FERTILIZE','有机施肥','腐熟有机肥底肥+叶面生物肥，全程零化学农药。','https://picsum.photos/seed/fertilize9/800/600','寿光蔬菜基地','山东寿光现代农业产业园','2026-08-25 09:00:00',2,2,'FARMER','2026-09-25 09:05:00'),
(9,'PEST','物理防虫','防虫网+粘虫板物理防控，全程无化学残留。','https://picsum.photos/seed/pest9/800/600','寿光蔬菜基地','山东寿光现代农业产业园','2026-09-05 10:00:00',3,2,'FARMER','2026-09-25 09:10:00'),
(9,'HARVEST','清晨采收','花球紧实期清晨采收，带露锁鲜，叶柄脆嫩。','https://picsum.photos/seed/harvest9/800/600','寿光蔬菜基地','山东寿光现代农业产业园','2026-09-27 05:00:00',4,2,'FARMER','2026-09-27 05:30:00'),
(9,'PROCESS','分拣装袋','按花球大小分级，切除多余叶柄，套网袋装盒。','https://picsum.photos/seed/process9/800/600','寿光蔬菜基地','山东寿光分拣车间','2026-09-27 06:00:00',5,2,'FARMER','2026-09-27 06:30:00'),
(9,'QUALITY','农残速检','采收后1小时完成农残快检，抑制率<40%，合格出厂。','https://picsum.photos/seed/quality9/800/600','寿光蔬菜基地','山东寿光检测站','2026-09-27 07:00:00',6,1,'ADMIN','2026-09-27 07:30:00'),
(9,'STORAGE','冷库预冷','0-4℃冷库预冷2小时，快速去除田间热，延长货架期。','https://picsum.photos/seed/storage9/800/600','橙心助农仓储中心','寿光冷库','2026-09-27 07:30:00',7,2,'FARMER','2026-09-27 07:40:00'),
(9,'LOGISTICS','冷链直发','冰袋+泡沫箱冷链，24小时短链配送到家。','https://picsum.photos/seed/logistics9/800/600','顺源冷链物流','寿光-山东全省','2026-09-27 09:00:00',8,2,'FARMER','2026-09-27 09:10:00'),
-- 批次10 五常黑米
(10,'PLANT','黑米种植','五常核心产区黑稻品种，拉林河活水灌溉，一年一季。','https://picsum.photos/seed/nursery10/800/600','五常稻花香农场','黑龙江五常市核心产区','2026-05-20 08:00:00',1,4,'FARMER','2026-09-26 09:00:00'),
(10,'FERTILIZE','有机肥田','腐熟有机肥+秸秆还田，土壤有机质含量3.2%。','https://picsum.photos/seed/fertilize10/800/600','五常稻花香农场','黑龙江五常市核心产区','2026-06-10 09:00:00',2,4,'FARMER','2026-09-26 09:05:00'),
(10,'PEST','绿色防控','稻田养鸭+杀虫灯物理防控，全程零化学农药。','https://picsum.photos/seed/pest10/800/600','五常稻花香农场','黑龙江五常市核心产区','2026-07-15 10:00:00',3,4,'FARMER','2026-09-26 09:10:00'),
(10,'HARVEST','霜前收割','10月初霜前收割，自然晾晒15天后低温烘干。','https://picsum.photos/seed/harvest10/800/600','五常稻花香农场','黑龙江五常市核心产区','2026-09-29 08:00:00',4,4,'FARMER','2026-09-29 09:00:00'),
(10,'PROCESS','低温烘干','低温烘干保留花青素营养，恒温脱壳，不抛光。','https://picsum.photos/seed/process10/800/600','五常稻花香农场','黑龙江五常加工车间','2026-09-30 10:00:00',5,4,'FARMER','2026-09-30 11:00:00'),
(10,'QUALITY','批次质检','水分≤14.5%，花青素含量检测达标，出具产地报告。','https://picsum.photos/seed/quality10/800/600','橙心助农质检中心','五常检测实验室','2026-09-30 15:00:00',6,1,'ADMIN','2026-09-30 15:30:00'),
(10,'STORAGE','低温仓储','15℃低温粮仓储存，避光防潮，保持花青素活性。','https://picsum.photos/seed/storage10/800/600','五常稻花香农场','五常低温粮仓','2026-09-30 16:00:00',7,4,'FARMER','2026-09-30 16:30:00'),
(10,'LOGISTICS','真空包装','真空锁鲜包装，防潮防虫，全国包邮3-5天送达。','https://picsum.photos/seed/logistics10/800/600','顺源冷链物流','五常-全国干线','2026-09-30 17:00:00',8,4,'FARMER','2026-09-30 17:30:00');

INSERT INTO `product` (`id`,`name`,`category_id`,`batch_id`,`price`,`stock`,`sales`,`cover_image`,`images`,`description`,`status`) VALUES
(1,'赣南脐橙 5斤装（可溯源）',1,1,39.90,500,1286,'http://localhost:8080/images/orange.jpg','http://localhost:8080/images/orange.jpg,orange_tree.jpg','江西赣南原产地直发，纽荷尔脐橙，甜度12°+，果肉细嫩化渣。每箱附溯源二维码，扫码可查种植、施肥、采收、质检全流程。',1),
(2,'烟台红富士苹果 10斤装（可溯源）',1,2,59.90,320,867,'http://localhost:8080/images/apple.jpg','http://localhost:8080/images/apple.jpg,apple_single.jpg','栖霞红富士，套袋种植，霜降后采收，糖心率高，脆甜多汁。全程冷链配送，溯源信息一码可查。',1),
(3,'有机菠菜 1斤装（可溯源）',2,3,12.90,800,2345,'http://localhost:8080/images/spinach.jpg','http://localhost:8080/images/spinach.jpg,spinach2.jpg','无土栽培有机菠菜，零化学农药，清晨采收冷链直发，采收到上架不超过24小时。',1),
(4,'五常稻花香大米 5kg装（可溯源）',3,4,89.90,260,652,'http://localhost:8080/images/rice.jpg','http://localhost:8080/images/rice.jpg,rice2.jpg','五常核心产区稻花香2号，一年一季，真空包装锁鲜，饭香浓郁、软糯回甘。',1),
(5,'赣南脐橙 10斤礼盒装（可溯源）',1,1,79.90,200,452,'http://localhost:8080/images/orange_tree.jpg','http://localhost:8080/images/orange_tree.jpg,orange.jpg','精选85mm以上大果礼盒装，适合馈赠亲友，附溯源卡与质检报告。',1),
(6,'五常胚芽米 2.5kg装（可溯源）',3,4,49.90,150,289,'http://localhost:8080/images/germ_rice.jpg','http://localhost:8080/images/germ_rice.jpg,germ_rice2.jpg','轻度碾磨保留胚芽，营养更完整，适合老人儿童食用。',1),
(7,'烟台红富士 5斤尝鲜装（可溯源）',1,2,39.90,400,198,'http://localhost:8080/images/apple_single.jpg','http://localhost:8080/images/apple_single.jpg,apple.jpg','果径75-80mm，尝鲜实惠装，品质与10斤装一致。',1),
(8,'果园散养土鸡蛋 30枚装（可溯源）',4,5,39.90,300,156,'http://localhost:8080/images/egg.jpg','http://localhost:8080/images/egg.jpg,egg2.jpg','赣南丘陵果园生态放养，谷物喂养，每日拾蛋、逐枚光照检测，蛋香浓郁无腥味。',1),
(9,'散养土鸡 整只2.5斤（可溯源）',4,6,69.90,120,89,'http://localhost:8080/images/chicken.jpg','http://localhost:8080/images/chicken.jpg,chicken2.jpg','果园散养180天足龄出栏，五谷喂养，检疫合格，冷鲜排酸锁鲜，煲汤鲜香。',1),
(10,'赣南白莲子 250g装（可溯源）',5,7,29.90,260,134,'http://localhost:8080/images/lotus.jpg','http://localhost:8080/images/lotus.jpg,lotus2.jpg','手工去芯、自然晾晒，颗粒饱满，炖汤煮粥清甜粉糯，无硫熏无添加。',1),
(11,'东北黑木耳 200g装（可溯源）',5,8,25.90,400,210,'http://localhost:8080/images/fungus.jpg','http://localhost:8080/images/fungus.jpg,fungus2.jpg','长白山椴木栽培，自然晾晒不硫熏，泡发率高，肉厚脆嫩，凉拌热炒皆宜。',1),
(12,'有机西兰花 1斤装（可溯源）',2,9,15.90,350,268,'http://localhost:8080/images/broccoli.jpg','http://localhost:8080/images/broccoli.jpg,broccoli2.jpg','有机栽培、物理防虫，清晨采收冷链直发，花球紧实，富含维生素。',1),
(13,'五常黑米 2.5kg装（可溯源）',3,10,45.90,200,96,'http://localhost:8080/images/blackrice.jpg','http://localhost:8080/images/blackrice.jpg,blackrice2.jpg','五常核心产区黑稻，低温烘干保留花青素，真空锁鲜，熬粥浓稠滋补。',1);

INSERT INTO `address` (`user_id`,`receiver`,`phone`,`province`,`city`,`district`,`detail`,`is_default`) VALUES
(5,'小王','13800000006','北京市','北京市','海淀区','中关村大街1号院3号楼502室',1);

INSERT INTO `cart` (`user_id`,`product_id`,`quantity`) VALUES
(5,1,2),
(5,4,1);

-- 演示订单（consumer01 的历史订单，分布在近7天，便于看板与日报/周报展示数据；含物流履约演示数据）
INSERT INTO `orders` (`id`,`order_no`,`user_id`,`total_amount`,`status`,`receiver`,`phone`,`address`,`remark`,`courier_company`,`tracking_no`,`delivery_status`,`pay_time`,`ship_time`,`complete_time`,`create_time`) VALUES
(1,'OF202609240001',5,79.80,'COMPLETED','小王','13800000006','北京市海淀区中关村大街1号院3号楼502室','','顺丰速运','SF13822024092401','SIGNED', '2026-09-24 10:12:00','2026-09-24 16:00:00','2026-09-26 09:30:00','2026-09-24 10:00:00'),
(2,'OF202609260002',5,89.90,'SHIPPED','小王','13800000006','北京市海淀区中关村大街1号院3号楼502室','请放门口','京东物流','JDVA2609260002','DELIVERING','2026-09-26 20:05:00','2026-09-27 10:00:00',NULL,'2026-09-26 20:00:00'),
(3,'OF202609280003',5,12.90,'PAID','小王','13800000006','北京市海淀区中关村大街1号院3号楼502室','',NULL,NULL,NULL,'2026-09-28 12:30:00',NULL,NULL,'2026-09-28 12:25:00'),
(4,'OF202609300004',5,59.90,'PENDING','小王','13800000006','北京市海淀区中关村大街1号院3号楼502室','',NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-30 09:40:00'),
(5,'OF202609301205',5,39.90,'PAID','小王','13800000006','北京市海淀区中关村大街1号院3号楼502室','今日下单演示',NULL,NULL,NULL,'2026-09-30 12:05:00',NULL,NULL,'2026-09-30 12:00:00');

INSERT INTO `order_item` (`order_id`,`product_id`,`product_name`,`product_image`,`price`,`quantity`,`amount`) VALUES
(1,1,'赣南脐橙 5斤装（可溯源）','http://localhost:8080/images/orange.jpg',39.90,2,79.80),
(2,4,'五常稻花香大米 5kg装（可溯源）','http://localhost:8080/images/rice.jpg',89.90,1,89.90),
(3,3,'有机菠菜 1斤装（可溯源）','http://localhost:8080/images/spinach.jpg',12.90,1,12.90),
(4,2,'烟台红富士苹果 10斤装（可溯源）','http://localhost:8080/images/apple.jpg',59.90,1,59.90),
(5,1,'赣南脐橙 5斤装（可溯源）','http://localhost:8080/images/orange.jpg',39.90,1,39.90);

-- 物流跟踪时间轴（订单1已完成签收；订单2配送中）
INSERT INTO `logistics_log` (`order_id`,`node_time`,`title`,`description`) VALUES
(1,'2026-09-24 16:00:00','已发货','快递公司：顺丰速运，运单号：SF13822024092401'),
(1,'2026-09-25 08:30:00','配送中','商品已由顺丰速运揽收，正在配送途中（运单号：SF13822024092401）'),
(1,'2026-09-26 09:00:00','已签收','商品已签收，感谢购买，欢迎扫码验证溯源信息'),
(2,'2026-09-27 10:00:00','已发货','快递公司：京东物流，运单号：JDVA2609260002'),
(2,'2026-09-28 07:30:00','配送中','商品已由京东物流揽收，正在配送途中（运单号：JDVA2609260002）');


-- ============================================================

-- ============================================================
-- 种子批次溯源链哈希存证（与 Java computeChainHash 算法一致）
-- ============================================================
-- 批次 1（赣南脐橙）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = '4114ada9acb69f4e1b05e64c243e20bae06a499cd643f4240017300e29526d4b' WHERE `id` = 1;

-- 批次 2（烟台红富士苹果）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = 'bcc748d935ee6e419aac9798ec4759fbd08d7b21be82b294c11942811d993369' WHERE `id` = 2;

-- 批次 3（有机菠菜）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = '5cd6174103c4329515620edb8b8e2c38e9ef5bb6391836640e02592c5ec5b324' WHERE `id` = 3;

-- 批次 4（五常稻花香大米）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = '42dcc06ad67ef1fa37eb360e2b4ecf5999eaecf59ecd3073f8a1756c63f4e207' WHERE `id` = 4;

-- 批次 5（果园散养土鸡蛋）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = 'cd2ede67dd19fcd0cc59598e2d262b4127cc02a75eb4d7ea807e57d49da29146' WHERE `id` = 5;

-- 批次 6（散养土鸡）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = 'c8f33c037d8d451efaa83a95b447797cd9910fed22ba68e4b85e75f8f4b9abbe' WHERE `id` = 6;

-- 批次 7（赣南白莲子）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = 'b6c4a903b5f1af55f0303e2c08e8049b826e3a163b484b15b8667ee4adee7167' WHERE `id` = 7;

-- 批次 8（东北黑木耳）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = '664349bfcd71ddf5f883ae0256017f907f9125098b54ba336e50993c78905662' WHERE `id` = 8;

-- 批次 9（有机西兰花）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = 'e6eb175ab8e0bcece754c473e4371a499d312bdae5b46f8c10b77bffab843e97' WHERE `id` = 9;

-- 批次 10（五常黑米）溯源链 SHA-256 摘要哈希
UPDATE `trace_batch` SET `hash_value` = '4f6bfb9b06f615b556200ec48908206d563d8eba247d6d14da2c44d89a5c566c' WHERE `id` = 10;
