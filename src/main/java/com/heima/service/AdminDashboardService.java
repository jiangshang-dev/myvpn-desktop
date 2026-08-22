package com.heima.service;

import com.heima.dto.AdminDtos;
import com.heima.entity.VpnNode;
import com.heima.entity.VpnSession;
import com.heima.mapper.VpnDeviceMapper;
import com.heima.mapper.VpnNodeMapper;
import com.heima.mapper.VpnSessionMapper;
import com.heima.mapper.VpnUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final VpnSessionMapper sessionMapper;
    private final VpnUserMapper userMapper;
    private final VpnDeviceMapper deviceMapper;
    private final VpnNodeMapper nodeMapper;
    private final VpnConnectService connectService;

    public Map<String, Object> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("stats", new AdminDtos.DashboardStats(
                sessionMapper.countOnline(),
                userMapper.countAll(),
                deviceMapper.listDevices(null, null).size(),
                nodeMapper.countEnabled()
        ));
        data.put("nodeOnline", sessionMapper.groupOnlineByNode());
        data.put("sessions", sessionMapper.listOnlineSessions());
        return data;
    }

    public List<AdminDtos.SessionRow> listSessions(String status, Long nodeId, String q) {
        return sessionMapper.listSessions(status, nodeId, q);
    }

    public List<AdminDtos.UserRow> listUsers(String q) {
        return userMapper.listUsers(q);
    }

    public List<AdminDtos.DeviceRow> listDevices(String q, Long userId) {
        return deviceMapper.listDevices(q, userId);
    }

    public List<AdminDtos.NodeRow> listNodes() {
        return nodeMapper.listAll();
    }

    public VpnNode getNode(Long id) {
        return nodeMapper.findById(id);
    }

    @Transactional
    public void toggleUser(Long id, Integer enabled) {
        userMapper.updateEnabled(id, enabled);
    }

    @Transactional
    public void toggleDevice(Long id, Integer enabled) {
        deviceMapper.updateEnabled(id, enabled);
        if (enabled != null && enabled == 0) {
            VpnSession active = sessionMapper.findActiveByDevice(id);
            if (active != null) connectService.kickSession(active.getId(), "device_disabled");
        }
    }

    @Transactional
    public void toggleNode(Long id, Integer enabled) {
        nodeMapper.updateEnabled(id, enabled);
    }

    public void kick(Long sessionId, String reason) {
        connectService.kickSession(sessionId, reason == null ? "admin_kick" : reason);
    }

    @Transactional
    public void saveNode(AdminDtos.NodeSaveRequest req) {
        if (req.name() == null || req.name().isBlank()) throw new IllegalArgumentException("请填写节点名称");
        if (req.host() == null || req.host().isBlank()) throw new IllegalArgumentException("请填写服务器地址");
        if (req.port() == null || req.port() <= 0) throw new IllegalArgumentException("请填写端口");
        if (req.userId() == null || req.userId().isBlank()) throw new IllegalArgumentException("请填写用户 ID (UUID)");
        String userId = req.userId().trim();
        if (!userId.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("用户 ID 必须是有效的 UUID 格式");
        }
        VpnNode node = new VpnNode();
        node.setId(req.id());
        node.setName(req.name().trim());
        node.setRegion(req.region() == null ? "" : req.region().trim());
        node.setHost(req.host().trim());
        node.setPort(req.port());
        node.setUserId(userId);
        node.setProtocol(req.protocol() == null ? "vmess" : req.protocol());
        node.setNetwork(req.network() == null ? "tcp" : req.network());
        node.setSecurity(req.security() == null ? "auto" : req.security());
        node.setHeaderType(req.headerType() == null || req.headerType().isBlank() ? "none" : req.headerType().trim());
        node.setHostHeader(req.hostHeader() == null ? "" : req.hostHeader().trim());
        node.setTls(req.tls() == null ? "" : req.tls().trim());
        node.setSni(req.sni());
        node.setPath(req.path() == null ? "" : req.path());
        node.setAlterId(req.alterId() == null ? 0 : req.alterId());
        node.setMaxOnline(req.maxOnline() == null ? 0 : req.maxOnline());
        node.setEnabled(req.enabled() == null ? 1 : req.enabled());
        node.setSortNo(req.sortNo() == null ? 0 : req.sortNo());
        node.setRemark(req.remark());
        if (req.id() == null) {
            nodeMapper.insert(node);
        } else {
            nodeMapper.update(node);
        }
    }
}
