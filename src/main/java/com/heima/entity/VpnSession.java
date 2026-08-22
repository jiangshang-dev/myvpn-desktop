package com.heima.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VpnSession {
    private Long id;
    private Long userId;
    private Long deviceId;
    private Long nodeId;
    private String status;
    private String clientIp;
    private Long uploadBytes;
    private Long downloadBytes;
    private Long uploadSpeed;
    private Long downloadSpeed;
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
    private String disconnectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
