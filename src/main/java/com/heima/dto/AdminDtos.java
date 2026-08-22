package com.heima.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {}

    public record LoginRequest(String account, String password) {}

    public record LoginResponse(String token, String account, String name) {}

    public record PasswordRequest(String oldPassword, String newPassword) {}

    public record DashboardStats(
            long onlineSessions,
            long totalUsers,
            long totalDevices,
            long enabledNodes
    ) {}

    public record NodeOnlineRow(
            Long nodeId,
            String nodeName,
            String region,
            long onlineCount,
            long totalUpload,
            long totalDownload
    ) {}

    public record SessionRow(
            Long id,
            Long userId,
            String email,
            String nickname,
            Long deviceId,
            String deviceName,
            String platform,
            Long nodeId,
            String nodeName,
            String status,
            String clientIp,
            Long uploadBytes,
            Long downloadBytes,
            Long uploadSpeed,
            Long downloadSpeed,
            LocalDateTime connectedAt,
            long durationSeconds
    ) {}

    public record UserRow(
            Long id,
            String email,
            String nickname,
            Integer enabled,
            LocalDateTime planExpireAt,
            LocalDateTime lastLoginAt,
            long deviceCount,
            long onlineCount
    ) {}

    public record DeviceRow(
            Long id,
            Long userId,
            String email,
            String deviceUuid,
            String deviceName,
            String platform,
            Integer enabled,
            String lastIp,
            LocalDateTime lastSeenAt,
            String onlineStatus
    ) {}

    public record NodeRow(
            Long id,
            String name,
            String region,
            String host,
            Integer port,
            String protocol,
            Integer enabled,
            Integer maxOnline,
            long onlineCount
    ) {}

    public record NodeSaveRequest(
            Long id,
            String name,
            String region,
            String host,
            Integer port,
            String userId,
            String protocol,
            String network,
            String security,
            String headerType,
            String hostHeader,
            String tls,
            String sni,
            String path,
            Integer alterId,
            Integer maxOnline,
            Integer enabled,
            Integer sortNo,
            String remark
    ) {}

    public record ToggleRequest(Long id, Integer enabled) {}

    public record KickRequest(Long sessionId, String reason) {}

    public record TcpTestResult(boolean reachable, long latencyMs, String error) {}

    public record ProxyTestResult(boolean ok, long latencyMs, long speedBytesPerSec, String error) {}

    public record NodeTestResult(
            Long nodeId,
            String nodeName,
            TcpTestResult tcp,
            ProxyTestResult proxy,
            List<String> warnings,
            Map<String, String> configSummary
    ) {}

    public record PageResult<T>(List<T> records, long total) {}
}
