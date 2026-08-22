package com.heima.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VpnDevice {
    private Long id;
    private Long userId;
    private String deviceUuid;
    private String deviceName;
    private String platform;
    private Integer enabled;
    private String v2rayUuid;
    private String lastIp;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
}
