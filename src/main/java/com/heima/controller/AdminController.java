package com.heima.controller;

import com.heima.dto.AdminDtos;
import com.heima.entity.VpnAdmin;
import com.heima.entity.VpnNode;
import com.heima.service.AdminAuthService;
import com.heima.service.AdminDashboardService;
import com.heima.service.NodeTestService;
import com.heima.web.AdminAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAuthService adminAuthService;
    private final AdminDashboardService dashboardService;
    private final NodeTestService nodeTestService;

    @PostMapping("/login")
    public AdminDtos.LoginResponse login(@RequestBody AdminDtos.LoginRequest req) {
        VpnAdmin admin = adminAuthService.login(req.account(), req.password());
        String token = adminAuthService.createSession(admin);
        return new AdminDtos.LoginResponse(token, admin.getAccount(), admin.getName());
    }

    @GetMapping("/me")
    public AdminDtos.LoginResponse me(HttpServletRequest request) {
        VpnAdmin admin = (VpnAdmin) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        return new AdminDtos.LoginResponse("", admin.getAccount(), admin.getName());
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            adminAuthService.logout(auth.substring(7).trim());
        }
        return Map.of("message", "ok");
    }

    @PostMapping("/password")
    public Map<String, String> password(HttpServletRequest request, @RequestBody AdminDtos.PasswordRequest req) {
        VpnAdmin admin = (VpnAdmin) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        adminAuthService.changePassword(admin.getId(), req.oldPassword(), req.newPassword());
        return Map.of("message", "ok");
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return dashboardService.dashboard();
    }

    @GetMapping("/sessions")
    public List<AdminDtos.SessionRow> sessions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(required = false) String q
    ) {
        return dashboardService.listSessions(status, nodeId, q);
    }

    @GetMapping("/users")
    public List<AdminDtos.UserRow> users(@RequestParam(required = false) String q) {
        return dashboardService.listUsers(q);
    }

    @GetMapping("/devices")
    public List<AdminDtos.DeviceRow> devices(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long userId
    ) {
        return dashboardService.listDevices(q, userId);
    }

    @GetMapping("/nodes")
    public List<AdminDtos.NodeRow> nodes() {
        return dashboardService.listNodes();
    }

    @PostMapping("/users/toggle")
    public Map<String, String> toggleUser(@RequestBody AdminDtos.ToggleRequest req) {
        dashboardService.toggleUser(req.id(), req.enabled());
        return Map.of("message", "ok");
    }

    @PostMapping("/devices/toggle")
    public Map<String, String> toggleDevice(@RequestBody AdminDtos.ToggleRequest req) {
        dashboardService.toggleDevice(req.id(), req.enabled());
        return Map.of("message", "ok");
    }

    @PostMapping("/nodes/toggle")
    public Map<String, String> toggleNode(@RequestBody AdminDtos.ToggleRequest req) {
        dashboardService.toggleNode(req.id(), req.enabled());
        return Map.of("message", "ok");
    }

    @GetMapping("/nodes/{id}")
    public VpnNode nodeDetail(@PathVariable Long id) {
        VpnNode node = dashboardService.getNode(id);
        if (node == null) throw new IllegalArgumentException("节点不存在");
        return node;
    }

    @PostMapping("/nodes/save")
    public Map<String, String> saveNode(@RequestBody AdminDtos.NodeSaveRequest req) {
        dashboardService.saveNode(req);
        return Map.of("message", "ok");
    }

    @PostMapping("/nodes/{id}/test")
    public AdminDtos.NodeTestResult testNode(@PathVariable Long id) {
        return nodeTestService.test(id);
    }

    @PostMapping("/nodes/test-all")
    public List<AdminDtos.NodeTestResult> testAllNodes() {
        return nodeTestService.testAll();
    }

    @PostMapping("/sessions/kick")
    public Map<String, String> kick(@RequestBody AdminDtos.KickRequest req) {
        dashboardService.kick(req.sessionId(), req.reason());
        return Map.of("message", "ok");
    }
}
