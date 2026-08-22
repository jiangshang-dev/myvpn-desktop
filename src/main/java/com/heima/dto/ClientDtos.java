package com.heima.dto;

import java.util.List;
import java.util.Map;

public final class ClientDtos {

    private ClientDtos() {}

    public record RegisterRequest(String email, String password, String nickname) {}

    public record LoginRequest(String email, String password, String deviceUuid, String deviceName, String platform) {}

    public record AuthResponse(String token, String email, String nickname, Long deviceId, String v2rayUuid) {}

    public record NodeView(
            Long id,
            String name,
            String region,
            String host,
            Integer port,
            String protocol,
            Integer onlineCount,
            boolean available
    ) {}

    public record ConnectRequest(Long nodeId) {}

    public record ConnectResponse(
            Long sessionId,
            String status,
            Map<String, Object> v2rayConfig,
            String shareLink
    ) {}

    public record HeartbeatRequest(
            Long sessionId,
            Long uploadBytes,
            Long downloadBytes,
            Long uploadSpeed,
            Long downloadSpeed
    ) {}

    public record HeartbeatResponse(String command, String message) {}

    public record DisconnectRequest(Long sessionId, String reason) {}

    public record NodeTestResult(
            Long nodeId,
            String nodeName,
            boolean tcpOk,
            long tcpLatencyMs,
            boolean proxyOk,
            long proxyLatencyMs,
            long speedBytesPerSec,
            String error,
            List<String> warnings
    ) {}
}
