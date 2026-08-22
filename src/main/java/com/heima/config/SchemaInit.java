package com.heima.config;

import com.heima.entity.VpnAdmin;
import com.heima.entity.VpnNode;
import com.heima.mapper.VpnAdminMapper;
import com.heima.mapper.VpnNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaInit implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final VpnAdminMapper adminMapper;
    private final VpnNodeMapper nodeMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        exec("""
            CREATE TABLE IF NOT EXISTS vpn_admin (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                account VARCHAR(64) NOT NULL UNIQUE,
                password VARCHAR(128) NOT NULL,
                name VARCHAR(64) NOT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        exec("""
            CREATE TABLE IF NOT EXISTS vpn_admin_session (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                admin_id BIGINT NOT NULL,
                token VARCHAR(64) NOT NULL UNIQUE,
                expire_at DATETIME NOT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_admin (admin_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        exec("""
            CREATE TABLE IF NOT EXISTS vpn_user (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                email VARCHAR(128) NOT NULL UNIQUE,
                password VARCHAR(128) NOT NULL,
                nickname VARCHAR(64) NOT NULL DEFAULT '',
                enabled TINYINT NOT NULL DEFAULT 1,
                plan_id BIGINT NULL,
                plan_expire_at DATETIME NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                last_login_at DATETIME NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        exec("""
            CREATE TABLE IF NOT EXISTS vpn_device (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                device_uuid VARCHAR(64) NOT NULL,
                device_name VARCHAR(128) NOT NULL DEFAULT '',
                platform VARCHAR(32) NOT NULL DEFAULT '',
                enabled TINYINT NOT NULL DEFAULT 1,
                v2ray_uuid VARCHAR(64) NOT NULL,
                last_ip VARCHAR(64) NULL,
                last_seen_at DATETIME NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_user_device (user_id, device_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        exec("""
            CREATE TABLE IF NOT EXISTS vpn_node (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(128) NOT NULL,
                region VARCHAR(64) NOT NULL DEFAULT '',
                host VARCHAR(255) NOT NULL,
                port INT NOT NULL DEFAULT 443,
                user_id VARCHAR(64) NOT NULL DEFAULT '',
                protocol VARCHAR(16) NOT NULL DEFAULT 'vmess',
                network VARCHAR(16) NOT NULL DEFAULT 'tcp',
                security VARCHAR(16) NOT NULL DEFAULT 'auto',
                header_type VARCHAR(16) NOT NULL DEFAULT 'none',
                host_header VARCHAR(255) NULL,
                tls VARCHAR(16) NOT NULL DEFAULT '',
                sni VARCHAR(255) NULL,
                path VARCHAR(255) NULL,
                alter_id INT NOT NULL DEFAULT 0,
                max_online INT NOT NULL DEFAULT 0,
                enabled TINYINT NOT NULL DEFAULT 1,
                sort_no INT NOT NULL DEFAULT 0,
                remark VARCHAR(255) NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        migrateNodeColumns();
        exec("""
            CREATE TABLE IF NOT EXISTS vpn_session (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                device_id BIGINT NOT NULL,
                node_id BIGINT NOT NULL,
                status VARCHAR(16) NOT NULL DEFAULT 'connecting',
                client_ip VARCHAR(64) NULL,
                upload_bytes BIGINT NOT NULL DEFAULT 0,
                download_bytes BIGINT NOT NULL DEFAULT 0,
                upload_speed BIGINT NOT NULL DEFAULT 0,
                download_speed BIGINT NOT NULL DEFAULT 0,
                connected_at DATETIME NULL,
                disconnected_at DATETIME NULL,
                disconnect_reason VARCHAR(128) NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        exec("""
            CREATE TABLE IF NOT EXISTS vpn_user_session (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                device_id BIGINT NOT NULL,
                token VARCHAR(64) NOT NULL UNIQUE,
                expire_at DATETIME NOT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        if (adminMapper.countAll() == 0) {
            VpnAdmin admin = new VpnAdmin();
            admin.setAccount("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("管理员");
            adminMapper.insert(admin);
        }

        if (nodeMapper.listAll().isEmpty()) {
            VpnNode node = new VpnNode();
            node.setName("美国-示例节点");
            node.setRegion("US");
            node.setHost("103.119.14.83");
            node.setPort(1234);
            node.setUserId("e9e4da6f-f569-47e2-90c0-366f512da0bd");
            node.setProtocol("vmess");
            node.setNetwork("tcp");
            node.setSecurity("auto");
            node.setHeaderType("none");
            node.setHostHeader("");
            node.setTls("");
            node.setSni("");
            node.setPath("");
            node.setAlterId(64);
            node.setMaxOnline(100);
            node.setEnabled(1);
            node.setSortNo(1);
            node.setRemark("与 v2rayN 配置一致，请按需修改");
            nodeMapper.insert(node);
        }
    }

    private void migrateNodeColumns() {
        addColumnIfMissing("vpn_node", "user_id", "VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'VMess用户ID'");
        addColumnIfMissing("vpn_node", "header_type", "VARCHAR(16) NOT NULL DEFAULT 'none' COMMENT 'tcp伪装类型'");
        addColumnIfMissing("vpn_node", "host_header", "VARCHAR(255) NULL COMMENT 'ws伪装域名'");
        addColumnIfMissing("vpn_node", "tls", "VARCHAR(16) NOT NULL DEFAULT '' COMMENT '传输层TLS'");
        try {
            jdbc.update("UPDATE vpn_node SET tls = 'tls' WHERE security = 'tls' AND (tls IS NULL OR tls = '')");
            jdbc.update("UPDATE vpn_node SET security = 'auto' WHERE security = 'tls'");
        } catch (Exception ignored) { /* ignore */ }
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count != null && count == 0) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void exec(String sql) {
        jdbc.execute(sql);
    }
}
