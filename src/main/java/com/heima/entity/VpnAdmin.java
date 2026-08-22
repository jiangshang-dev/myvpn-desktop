package com.heima.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VpnAdmin {
    private Long id;
    private String account;
    private String password;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
