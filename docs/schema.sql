-- MyVPN 数据库设计（表前缀 vpn_）
-- 字符集 utf8mb4

CREATE DATABASE IF NOT EXISTS myvpn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE myvpn;

-- 管理员
CREATE TABLE IF NOT EXISTS vpn_admin (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    account     VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt',
    name        VARCHAR(64)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vpn_admin_session (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id    BIGINT       NOT NULL,
    token       VARCHAR(64)  NOT NULL UNIQUE,
    expire_at   DATETIME     NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin (admin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 终端用户
CREATE TABLE IF NOT EXISTS vpn_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    email           VARCHAR(128) NOT NULL UNIQUE,
    password        VARCHAR(128) NOT NULL COMMENT 'BCrypt',
    nickname        VARCHAR(64)  NOT NULL DEFAULT '',
    enabled         TINYINT      NOT NULL DEFAULT 1,
    plan_id         BIGINT       NULL,
    plan_expire_at  DATETIME     NULL COMMENT '套餐到期时间',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   DATETIME     NULL,
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户设备（每台电脑/手机一条）
CREATE TABLE IF NOT EXISTS vpn_device (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    device_uuid     VARCHAR(64)  NOT NULL COMMENT '客户端生成的设备唯一标识',
    device_name     VARCHAR(128) NOT NULL DEFAULT '',
    platform        VARCHAR(32)  NOT NULL DEFAULT '' COMMENT 'windows/macos/ios/android',
    enabled         TINYINT      NOT NULL DEFAULT 1,
    v2ray_uuid      VARCHAR(64)  NOT NULL COMMENT '该设备在节点上的 UUID',
    last_ip         VARCHAR(64)  NULL,
    last_seen_at    DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_device (user_id, device_uuid),
    INDEX idx_user (user_id),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- VPN 节点（美国服务器等）
CREATE TABLE IF NOT EXISTS vpn_node (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(128) NOT NULL COMMENT '显示名，如美国-洛杉矶',
    region          VARCHAR(64)  NOT NULL DEFAULT '',
    host            VARCHAR(255) NOT NULL COMMENT '节点域名或 IP',
    port            INT          NOT NULL DEFAULT 443,
    user_id         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'VMess 用户 ID (UUID)',
    protocol        VARCHAR(16)  NOT NULL DEFAULT 'vmess' COMMENT 'vmess/vless',
    network         VARCHAR(16)  NOT NULL DEFAULT 'tcp' COMMENT 'tcp/ws/grpc',
    security        VARCHAR(16)  NOT NULL DEFAULT 'auto' COMMENT 'VMess 加密: auto/aes-128-gcm/chacha20-poly1305/none',
    header_type     VARCHAR(16)  NOT NULL DEFAULT 'none' COMMENT 'tcp 伪装类型',
    host_header     VARCHAR(255) NULL COMMENT 'ws/http 伪装域名',
    tls             VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '传输层 TLS: 空=无, tls=启用',
    sni             VARCHAR(255) NULL,
    path            VARCHAR(255) NULL COMMENT 'ws path',
    alter_id        INT          NOT NULL DEFAULT 0,
    max_online      INT          NOT NULL DEFAULT 0 COMMENT '0=不限',
    enabled         TINYINT      NOT NULL DEFAULT 1,
    sort_no         INT          NOT NULL DEFAULT 0,
    remark          VARCHAR(255) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 连接会话（在线与历史）
CREATE TABLE IF NOT EXISTS vpn_session (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL,
    device_id           BIGINT       NOT NULL,
    node_id             BIGINT       NOT NULL,
    status              VARCHAR(16)  NOT NULL DEFAULT 'connecting' COMMENT 'connecting/online/disconnected/kicked',
    client_ip           VARCHAR(64)  NULL,
    upload_bytes        BIGINT       NOT NULL DEFAULT 0,
    download_bytes      BIGINT       NOT NULL DEFAULT 0,
    upload_speed        BIGINT       NOT NULL DEFAULT 0 COMMENT 'bytes/s',
    download_speed      BIGINT       NOT NULL DEFAULT 0 COMMENT 'bytes/s',
    connected_at        DATETIME     NULL,
    disconnected_at     DATETIME     NULL,
    disconnect_reason   VARCHAR(128) NULL COMMENT 'user/admin/kick/disable/timeout',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_device (device_id),
    INDEX idx_node (node_id),
    INDEX idx_status (status),
    INDEX idx_connected (connected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户会话 token
CREATE TABLE IF NOT EXISTS vpn_user_session (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    device_id   BIGINT       NOT NULL,
    token       VARCHAR(64)  NOT NULL UNIQUE,
    expire_at   DATETIME     NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 套餐（可选）
CREATE TABLE IF NOT EXISTS vpn_plan (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(64)  NOT NULL,
    duration_days   INT          NOT NULL DEFAULT 30,
    max_devices     INT          NOT NULL DEFAULT 2,
    enabled         TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作审计
CREATE TABLE IF NOT EXISTS vpn_audit_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_type  VARCHAR(16)  NOT NULL COMMENT 'admin/user/system',
    actor_id    BIGINT       NULL,
    action      VARCHAR(64)  NOT NULL,
    target_type VARCHAR(32)  NULL,
    target_id   BIGINT       NULL,
    detail      VARCHAR(512) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
