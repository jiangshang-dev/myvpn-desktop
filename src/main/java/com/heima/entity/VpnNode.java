package com.heima.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VpnNode {
    private Long id;
    private String name;
    private String region;
    private String host;
    private Integer port;
    private String userId;
    private String protocol;
    private String network;
    private String security;
    private String headerType;
    private String hostHeader;
    private String tls;
    private String sni;
    private String path;
    private Integer alterId;
    private Integer maxOnline;
    private Integer enabled;
    private Integer sortNo;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
