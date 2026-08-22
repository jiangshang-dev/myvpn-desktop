package com.heima.service;

import com.heima.dto.ClientDtos;
import com.heima.entity.VpnDevice;
import com.heima.entity.VpnNode;
import com.heima.entity.VpnSession;
import com.heima.entity.VpnUser;
import com.heima.mapper.VpnDeviceMapper;
import com.heima.mapper.VpnNodeMapper;
import com.heima.mapper.VpnSessionMapper;
import com.heima.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class VpnConnectService {

    private final VpnNodeMapper nodeMapper;
    private final VpnSessionMapper sessionMapper;
    private final VpnDeviceMapper deviceMapper;
    private final VpnRedisService redisService;

    public List<ClientDtos.NodeView> listNodes() {
        List<VpnNode> nodes = nodeMapper.listEnabled();
        List<ClientDtos.NodeView> views = new ArrayList<>();
        for (VpnNode node : nodes) {
            long online = sessionMapper.countOnlineByNode(node.getId());
            redisService.cacheNodeOnline(node.getId(), online);
            boolean available = node.getMaxOnline() == null || node.getMaxOnline() == 0 || online < node.getMaxOnline();
            views.add(new ClientDtos.NodeView(
                    node.getId(), node.getName(), node.getRegion(), node.getHost(), node.getPort(),
                    node.getProtocol(), (int) online, available
            ));
        }
        return views;
    }

    @Transactional
    public ClientDtos.ConnectResponse connect(VpnUser user, VpnDevice device, Long nodeId, HttpServletRequest request) {
        if (device.getEnabled() == 0) throw new IllegalArgumentException("设备已禁用");
        VpnNode node = nodeMapper.findById(nodeId);
        if (node == null || node.getEnabled() == 0) throw new IllegalArgumentException("节点不可用");
        long online = sessionMapper.countOnlineByNode(nodeId);
        if (node.getMaxOnline() != null && node.getMaxOnline() > 0 && online >= node.getMaxOnline()) {
            throw new IllegalArgumentException("节点已满，请稍后再试");
        }
        VpnSession active = sessionMapper.findActiveByDevice(device.getId());
        if (active != null) {
            sessionMapper.updateStatus(active.getId(), "disconnected", "reconnect");
            redisService.removeSession(active.getId());
        }
        VpnSession session = new VpnSession();
        session.setUserId(user.getId());
        session.setDeviceId(device.getId());
        session.setNodeId(nodeId);
        session.setStatus("connecting");
        session.setClientIp(ClientIp.resolve(request));
        sessionMapper.insert(session);
        sessionMapper.markOnline(session.getId(), session.getClientIp());
        redisService.touchSession(session.getId(), 0, 0);
        Map<String, Object> config = buildV2rayConfig(node, device);
        String shareLink = buildShareLink(node, device);
        return new ClientDtos.ConnectResponse(session.getId(), "online", config, shareLink);
    }

    public ClientDtos.HeartbeatResponse heartbeat(
            VpnDevice device, ClientDtos.HeartbeatRequest req, HttpServletRequest request
    ) {
        VpnSession session = sessionMapper.findById(req.sessionId());
        if (session == null || !Objects.equals(session.getDeviceId(), device.getId())) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!"online".equals(session.getStatus()) && !"connecting".equals(session.getStatus())) {
            throw new IllegalArgumentException("会话已结束");
        }
        String ip = ClientIp.resolve(request);
        sessionMapper.updateHeartbeat(
                session.getId(),
                nz(req.uploadBytes()), nz(req.downloadBytes()),
                nz(req.uploadSpeed()), nz(req.downloadSpeed()), ip
        );
        deviceMapper.updateLastSeen(device.getId(), ip);
        redisService.touchSession(session.getId(), nz(req.uploadSpeed()), nz(req.downloadSpeed()));
        String cmd = redisService.pollCommand(device.getId());
        if ("KICK".equals(cmd)) {
            sessionMapper.updateStatus(session.getId(), "kicked", "admin_kick");
            redisService.removeSession(session.getId());
            return new ClientDtos.HeartbeatResponse("KICK", "管理员已断开连接");
        }
        return new ClientDtos.HeartbeatResponse("NONE", "");
    }

    @Transactional
    public void disconnect(VpnDevice device, Long sessionId, String reason) {
        VpnSession session = sessionMapper.findById(sessionId);
        if (session == null || !Objects.equals(session.getDeviceId(), device.getId())) return;
        sessionMapper.updateStatus(sessionId, "disconnected", reason == null ? "user" : reason);
        redisService.removeSession(sessionId);
    }

    public void kickSession(Long sessionId, String reason) {
        VpnSession session = sessionMapper.findById(sessionId);
        if (session == null) return;
        sessionMapper.updateStatus(sessionId, "kicked", reason);
        redisService.removeSession(sessionId);
        redisService.kickDevice(session.getDeviceId());
    }

    /** 供节点测速使用：按节点配置生成 outbound（不依赖真实设备 UUID） */
    public Map<String, Object> buildTestOutbound(VpnNode node) {
        VpnDevice stub = new VpnDevice();
        stub.setV2rayUuid("");
        return buildV2rayConfig(node, stub);
    }

    private Map<String, Object> buildV2rayConfig(VpnNode node, VpnDevice device) {
        String uuid = resolveUserId(node, device);
        Map<String, Object> outbound = new LinkedHashMap<>();
        outbound.put("protocol", node.getProtocol());
        Map<String, Object> settings = new LinkedHashMap<>();
        List<Map<String, Object>> vnext = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("address", node.getHost());
        server.put("port", node.getPort());
        List<Map<String, Object>> users = new ArrayList<>();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", uuid);
        user.put("alterId", node.getAlterId() == null ? 0 : node.getAlterId());
        user.put("security", node.getSecurity() == null ? "auto" : node.getSecurity());
        users.add(user);
        server.put("users", users);
        vnext.add(server);
        settings.put("vnext", vnext);
        outbound.put("settings", settings);

        Map<String, Object> streamSettings = new LinkedHashMap<>();
        String network = node.getNetwork() == null ? "tcp" : node.getNetwork();
        streamSettings.put("network", network);
        if ("ws".equalsIgnoreCase(network)) {
            Map<String, Object> ws = new LinkedHashMap<>();
            ws.put("path", node.getPath() == null || node.getPath().isBlank() ? "/" : node.getPath());
            if (node.getHostHeader() != null && !node.getHostHeader().isBlank()) {
                Map<String, Object> headers = new LinkedHashMap<>();
                headers.put("Host", node.getHostHeader());
                ws.put("headers", headers);
            }
            streamSettings.put("wsSettings", ws);
        } else if ("tcp".equalsIgnoreCase(network)) {
            Map<String, Object> tcp = new LinkedHashMap<>();
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("type", node.getHeaderType() == null || node.getHeaderType().isBlank() ? "none" : node.getHeaderType());
            tcp.put("header", header);
            streamSettings.put("tcpSettings", tcp);
        }
        if (isTlsEnabled(node)) {
            streamSettings.put("security", "tls");
            Map<String, Object> tls = new LinkedHashMap<>();
            tls.put("serverName", node.getSni() == null || node.getSni().isBlank() ? node.getHost() : node.getSni());
            streamSettings.put("tlsSettings", tls);
        }
        outbound.put("streamSettings", streamSettings);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("outbound", outbound);
        root.put("remarks", node.getName());
        root.put("userId", uuid);
        return root;
    }

    private String buildShareLink(VpnNode node, VpnDevice device) {
        String uuid = resolveUserId(node, device);
        String network = node.getNetwork() == null ? "tcp" : node.getNetwork();
        String headerType = node.getHeaderType() == null || node.getHeaderType().isBlank() ? "none" : node.getHeaderType();
        String hostHeader = node.getHostHeader() == null ? "" : node.getHostHeader();
        String path = node.getPath() == null ? "" : node.getPath();
        String tls = isTlsEnabled(node) ? "tls" : "";
        return "vmess://" + Base64.getEncoder().encodeToString(
                String.format(
                        "{\"v\":\"2\",\"ps\":\"%s\",\"add\":\"%s\",\"port\":\"%d\",\"id\":\"%s\",\"aid\":\"%d\",\"scy\":\"%s\",\"net\":\"%s\",\"type\":\"%s\",\"host\":\"%s\",\"path\":\"%s\",\"tls\":\"%s\"}",
                        node.getName(), node.getHost(), node.getPort(), uuid,
                        node.getAlterId() == null ? 0 : node.getAlterId(),
                        node.getSecurity() == null ? "auto" : node.getSecurity(),
                        network, headerType, hostHeader, path, tls
                ).getBytes()
        );
    }

    private String resolveUserId(VpnNode node, VpnDevice device) {
        if (node.getUserId() != null && !node.getUserId().isBlank()) {
            return node.getUserId();
        }
        return device.getV2rayUuid();
    }

    private boolean isTlsEnabled(VpnNode node) {
        if (node.getTls() != null && "tls".equalsIgnoreCase(node.getTls())) return true;
        return node.getSecurity() != null && "tls".equalsIgnoreCase(node.getSecurity());
    }

    private long nz(Long v) {
        return v == null ? 0 : v;
    }
}
