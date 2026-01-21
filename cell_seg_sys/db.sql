/*
 Navicat Premium Data Transfer
 Source Server Type    : MySQL
 Source Server Version : 8.0.30 (建议 5.7 或 8.0+)
 Source Schema         : urine_analysis_db

 Target Server Type    : MySQL
 File Encoding         : 65001 (UTF-8)

 Date: 2026-01-13
 Description: 尿液有形成分识别系统数据库脚本 (轻量化版)
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 创建数据库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `urine_analysis_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `urine_analysis_db`;

-- ----------------------------
-- 2. 表结构：系统用户表 (sys_user)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录密码 (存储加密后的哈希值)',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户昵称 (显示用)',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户头像URL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE COMMENT '用户名唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户表：用于管理使用系统的研究人员或医生' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 3. 表结构：分析记录表 (analysis_record)
-- ----------------------------
DROP TABLE IF EXISTS `analysis_record`;
CREATE TABLE `analysis_record`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '归属用户ID (关联 sys_user.user_id)',
  `sample_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '样本名称/备注 (如：实验组A-001)',
  `image_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始图片存储路径 (相对路径或完整URL)',
  `result_image_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'AI处理后的结果图路径 (可选)',
  `summary_json` json NULL COMMENT '统计结果摘要 (JSON格式，例如 {"RBC": 20, "WBC": 5})',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '分析状态：0-识别中, 1-识别完成, 2-识别失败',
  `is_favorite` tinyint NOT NULL DEFAULT 0 COMMENT '是否收藏/星标：0-否, 1-是',
  `detect_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建/检测时间',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `detect_time` DESC) USING BTREE COMMENT '索引：查询某用户的历史记录，按时间倒序'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '分析记录主表：每一次图片上传生成一条记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 4. 表结构：分析详情表 (analysis_detail)
-- ----------------------------
DROP TABLE IF EXISTS `analysis_detail`;
CREATE TABLE `analysis_detail`  (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` bigint NOT NULL COMMENT '关联记录ID (关联 analysis_record.record_id)',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '识别类别 (例如：rbc, wbc, crystal)',
  `confidence` decimal(6, 4) NOT NULL COMMENT '置信度 (0.0000 - 1.0000)',
  `box_x` int NOT NULL COMMENT '边界框中心点 X坐标 (像素)',
  `box_y` int NOT NULL COMMENT '边界框中心点 Y坐标 (像素)',
  `box_w` int NOT NULL COMMENT '边界框宽度 (像素)',
  `box_h` int NOT NULL COMMENT '边界框高度 (像素)',
  `mask_points` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '实例分割掩码点集 (JSON字符串，用于前端Canvas绘制轮廓)',
  PRIMARY KEY (`detail_id`) USING BTREE,
  INDEX `idx_record_id`(`record_id` ASC) USING BTREE COMMENT '索引：根据记录ID快速查询所有细胞详情'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '分析详情表：存储AI识别出的每一个细胞的具体坐标和掩码' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 5. 插入初始化测试数据
-- ----------------------------

-- 插入一个默认管理员账号 (密码通常是 '123456' 的加密串，这里为了演示存明文，实际开发请用 BCrypt 加密)
INSERT INTO `sys_user` (`username`, `password`, `nickname`) VALUES ('admin', '123456', '系统管理员');

-- 插入一条模拟的“进行中”记录
INSERT INTO `analysis_record` (`user_id`, `sample_name`, `image_url`, `status`) 
VALUES (100, '测试样本-等待中', '/uploads/2026/01/13/test01.jpg', 0);

-- 插入一条模拟的“已完成”记录
INSERT INTO `analysis_record` (`user_id`, `sample_name`, `image_url`, `result_image_url`, `summary_json`, `status`, `detect_time`) 
VALUES (100, '测试样本-已完成', '/uploads/2026/01/13/test02.jpg', '/uploads/2026/01/13/test02_res.jpg', '{"rbc": 2, "wbc": 1}', 1, NOW());

-- 为上面那条“已完成”的记录插入详情数据 (假设 record_id 是 2)
-- 注意：这里假设 record_id 自动生成为 2，实际运行时请以数据库为准
INSERT INTO `analysis_detail` (`record_id`, `class_name`, `confidence`, `box_x`, `box_y`, `box_w`, `box_h`, `mask_points`) 
VALUES 
(2, 'rbc', 0.9520, 100, 200, 50, 50, '[[80,180],[120,180],[120,220],[80,220]]'),
(2, 'rbc', 0.9310, 300, 400, 55, 55, '[[280,380],[320,380],[320,420],[280,420]]'),
(2, 'wbc', 0.8850, 500, 100, 80, 80, '[[460,60],[540,60],[540,140],[460,140]]');

SET FOREIGN_KEY_CHECKS = 1;