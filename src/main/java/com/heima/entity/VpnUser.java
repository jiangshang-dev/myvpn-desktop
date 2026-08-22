package com.heima.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VpnUser {
    private Long id;
    private String email;
    private String password;
    private String nickname;
    private Integer enabled;
    private Long planId;
    private LocalDateTime planExpireAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
