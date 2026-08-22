/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80029 (8.0.29)
 Source Host           : 10.211.55.2:3306
 Source Schema         : myvpn

 Target Server Type    : MySQL
 Target Server Version : 80029 (8.0.29)
 File Encoding         : 65001

 Date: 22/08/2026 20:04:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for vpn_admin
-- ----------------------------
DROP TABLE IF EXISTS `vpn_admin`;
CREATE TABLE `vpn_admin`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `account`(`account` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vpn_admin
-- ----------------------------
INSERT INTO `vpn_admin` VALUES (1, 'admin', '$2a$10$LqkLmMLziXZjugoSPATXSekE/E6avxrZQy8SPsBbZ2U9Q/14yklJG', '管理员', '2026-08-22 11:12:19', '2026-08-22 11:12:19');

-- ----------------------------
-- Table structure for vpn_admin_session
-- ----------------------------
DROP TABLE IF EXISTS `vpn_admin_session`;
CREATE TABLE `vpn_admin_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL,
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `expire_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `token`(`token` ASC) USING BTREE,
  INDEX `idx_admin`(`admin_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vpn_admin_session
-- ----------------------------
INSERT INTO `vpn_admin_session` VALUES (1, 1, '801d8c22bb8543f985a6f6b2d8032686', '2026-08-29 19:12:34', '2026-08-22 11:12:34');

-- ----------------------------
-- Table structure for vpn_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `vpn_audit_log`;
CREATE TABLE `vpn_audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'admin/user/system',
  `actor_id` bigint NULL DEFAULT NULL,
  `action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `target_id` bigint NULL DEFAULT NULL,
  `detail` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vpn_audit_log
-- ----------------------------

-- ----------------------------
-- Table structure for vpn_device
-- ----------------------------
DROP TABLE IF EXISTS `vpn_device`;
CREATE TABLE `vpn_device`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户端生成的设备唯一标识',
  `device_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `platform` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'windows/macos/ios/android',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `v2ray_uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '该设备在节点上的 UUID',
  `last_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `last_seen_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_device`(`user_id` ASC, `device_uuid` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vpn_device
-- ----------------------------
INSERT INTO `vpn_device` VALUES (1, 2, 'e8e493e6-0dbd-4037-97b8-2afba62ef1e7', 'jiangxiaobaideMacBook-Pro.local', 'macos', 1, '21c4ef79-a844-4ac7-8e1b-ce6abb79f496', '127.0.0.1', '2026-08-22 12:01:33', '2026-08-22 11:22:34');

-- ----------------------------
-- Table structure for vpn_node
-- ----------------------------
DROP TABLE IF EXISTS `vpn_node`;
CREATE TABLE `vpn_node`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名，如美国-洛杉矶',
  `region` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点域名或 IP',
  `port` int NOT NULL DEFAULT 443,
  `protocol` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'vmess' COMMENT 'vmess/vless',
  `network` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ws' COMMENT 'tcp/ws/grpc',
  `security` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'auto' COMMENT 'auto/tls/none',
  `sni` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'ws path',
  `alter_id` int NOT NULL DEFAULT 0,
  `max_online` int NOT NULL DEFAULT 0 COMMENT '0=不限',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `sort_no` int NOT NULL DEFAULT 0,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'VMess用户ID',
  `header_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'none' COMMENT 'tcp伪装类型',
  `host_header` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'ws伪装域名',
  `tls` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '传输层TLS',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vpn_node
-- ----------------------------
INSERT INTO `vpn_node` VALUES (1, '美国-节点', 'US', '103.119.14.83', 1234, 'vmess', 'tcp', 'auto', NULL, '/v2ray', 64, 100, 1, 1, '请改成你的 v2ray 节点信息', '2026-08-22 11:12:19', '2026-08-22 11:52:00', 'e9e4da6f-f569-47e2-90c0-366f512da0bd', 'none', '', '');

-- ----------------------------
-- Table structure for vpn_plan
-- ----------------------------
DROP TABLE IF EXISTS `vpn_plan`;
CREATE TABLE `vpn_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `duration_days` int NOT NULL DEFAULT 30,
  `max_devices` int NOT NULL DEFAULT 2,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vpn_plan
-- ----------------------------

-- ----------------------------
-- Table structure for vpn_session
-- ----------------------------
DROP TABLE IF EXISTS `vpn_session`;
CREATE TABLE `vpn_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `node_id` bigint NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'connecting' COMMENT 'connecting/online/disconnected/kicked',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `upload_bytes` bigint NOT NULL DEFAULT 0,
  `download_bytes` bigint NOT NULL DEFAULT 0,
  `upload_speed` bigint NOT NULL DEFAULT 0 COMMENT 'bytes/s',
  `download_speed` bigint NOT NULL DEFAULT 0 COMMENT 'bytes/s',
  `connected_at` datetime NULL DEFAULT NULL,
  `disconnected_at` datetime NULL DEFAULT NULL,
  `disconnect_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'user/admin/kick/disable/timeout',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_device`(`device_id` ASC) USING BTREE,
  INDEX `idx_node`(`node_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_connected`(`connected_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vpn_session
-- ----------------------------
INSERT INTO `vpn_session` VALUES (1, 2, 1, 1, 'disconnected', '127.0.0.1', 312208, 1479773, 22128, 222294, '2026-08-22 11:22:38', '2026-08-22 11:26:48', 'user', '2026-08-22 11:22:38', '2026-08-22 11:26:48');
INSERT INTO `vpn_session` VALUES (2, 2, 1, 1, 'disconnected', '127.0.0.1', 0, 0, 0, 0, '2026-08-22 11:26:52', '2026-08-22 11:26:54', 'user', '2026-08-22 11:26:52', '2026-08-22 11:26:54');
INSERT INTO `vpn_session` VALUES (3, 2, 1, 1, 'disconnected', '127.0.0.1', 0, 0, 0, 0, '2026-08-22 11:29:20', '2026-08-22 11:29:29', 'user', '2026-08-22 11:29:20', '2026-08-22 11:29:29');
INSERT INTO `vpn_session` VALUES (4, 2, 1, 1, 'disconnected', '127.0.0.1', 625675, 1604439, 30322, 167163, '2026-08-22 11:29:30', '2026-08-22 11:37:42', 'reconnect', '2026-08-22 11:29:30', '2026-08-22 11:37:42');
INSERT INTO `vpn_session` VALUES (5, 2, 1, 1, 'disconnected', '127.0.0.1', 203529, 447073, 11740, 13212, '2026-08-22 11:37:42', '2026-08-22 11:40:14', 'user', '2026-08-22 11:37:42', '2026-08-22 11:40:14');
INSERT INTO `vpn_session` VALUES (6, 2, 1, 1, 'disconnected', '127.0.0.1', 59168, 335369, 44818, 126632, '2026-08-22 11:40:15', '2026-08-22 11:41:37', 'user', '2026-08-22 11:40:15', '2026-08-22 11:41:37');
INSERT INTO `vpn_session` VALUES (7, 2, 1, 1, 'disconnected', '127.0.0.1', 417085, 1574758, 65482, 44654, '2026-08-22 11:41:38', '2026-08-22 11:46:16', 'user', '2026-08-22 11:41:38', '2026-08-22 11:46:16');
INSERT INTO `vpn_session` VALUES (8, 2, 1, 1, 'disconnected', '127.0.0.1', 94143, 390044, 10241, 194030, '2026-08-22 11:46:17', '2026-08-22 11:47:34', 'user', '2026-08-22 11:46:17', '2026-08-22 11:47:34');
INSERT INTO `vpn_session` VALUES (9, 2, 1, 1, 'disconnected', '127.0.0.1', 262325, 1117881, 52009, 202971, '2026-08-22 11:47:35', '2026-08-22 11:50:18', 'user', '2026-08-22 11:47:35', '2026-08-22 11:50:18');
INSERT INTO `vpn_session` VALUES (10, 2, 1, 1, 'disconnected', '127.0.0.1', 177043, 515468, 8135, 119683, '2026-08-22 11:50:34', '2026-08-22 11:52:09', 'user', '2026-08-22 11:50:34', '2026-08-22 11:52:09');
INSERT INTO `vpn_session` VALUES (11, 2, 1, 1, 'disconnected', '127.0.0.1', 143159, 453379, 60719, 186029, '2026-08-22 11:52:15', '2026-08-22 11:53:21', 'user', '2026-08-22 11:52:15', '2026-08-22 11:53:21');
INSERT INTO `vpn_session` VALUES (12, 2, 1, 1, 'online', '127.0.0.1', 904893, 2815321, 7365, 48458, '2026-08-22 11:53:30', NULL, NULL, '2026-08-22 11:53:30', '2026-08-22 12:01:33');

-- ----------------------------
-- Table structure for vpn_user
-- ----------------------------
DROP TABLE IF EXISTS `vpn_user`;
CREATE TABLE `vpn_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `plan_id` bigint NULL DEFAULT NULL,
  `plan_expire_at` datetime NULL DEFAULT NULL COMMENT '套餐到期时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_login_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vpn_user
-- ----------------------------
INSERT INTO `vpn_user` VALUES (1, 'test-fix@qq.com', '$2a$10$NSwkf8WnMD3LhIn86r9xFu2Imcz5xM1aIlW2x4AZO5FYiJFa6S21S', 'test', 1, NULL, NULL, '2026-08-22 11:22:21', NULL);
INSERT INTO `vpn_user` VALUES (2, 'abc@qq.com', '$2a$10$SuEuvPQPjlvB2NshBPDlWuAWjzsku6L7mAMWnP5qCBP4qLqP8EQPS', 'abc', 1, NULL, NULL, '2026-08-22 11:22:34', '2026-08-22 11:22:34');

-- ----------------------------
-- Table structure for vpn_user_session
-- ----------------------------
DROP TABLE IF EXISTS `vpn_user_session`;
CREATE TABLE `vpn_user_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `expire_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `token`(`token` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_device`(`device_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vpn_user_session
-- ----------------------------
INSERT INTO `vpn_user_session` VALUES (1, 2, 1, '391565299f1244cca4c22993543a2dcb', '2026-09-21 19:22:34', '2026-08-22 11:22:34');

SET FOREIGN_KEY_CHECKS = 1;
