package com.heima.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VpnUserSession {
    private Long id;
    private Long userId;
    private Long deviceId;
    private String token;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
