package com.heima.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VpnAdminSession {
    private Long id;
    private Long adminId;
    private String token;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
