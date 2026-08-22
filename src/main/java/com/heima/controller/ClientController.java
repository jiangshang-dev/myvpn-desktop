package com.heima.controller;

import com.heima.dto.ClientDtos;
import com.heima.entity.VpnDevice;
import com.heima.entity.VpnUser;
import com.heima.service.ClientAuthService;
import com.heima.service.NodeTestService;
import com.heima.service.VpnConnectService;
import com.heima.web.ClientAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private final ClientAuthService authService;
    private final VpnConnectService connectService;
    private final NodeTestService nodeTestService;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/register")
    public ClientDtos.AuthResponse register(@RequestBody ClientDtos.RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public ClientDtos.AuthResponse login(@RequestBody ClientDtos.LoginRequest req, HttpServletRequest request) {
        return authService.login(req, request);
    }

    @GetMapping("/me")
    public ClientDtos.AuthResponse me(HttpServletRequest request) {
        VpnUser user = (VpnUser) request.getAttribute(ClientAuthInterceptor.ATTR_USER);
        VpnDevice device = (VpnDevice) request.getAttribute(ClientAuthInterceptor.ATTR_DEVICE);
        return new ClientDtos.AuthResponse("", user.getEmail(), user.getNickname(), device.getId(), device.getV2rayUuid());
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            authService.logout(auth.substring(7).trim());
        }
        return Map.of("message", "ok");
    }

    @GetMapping("/nodes")
    public List<ClientDtos.NodeView> nodes() {
        return connectService.listNodes();
    }

    @PostMapping("/nodes/{id}/test")
    public ClientDtos.NodeTestResult testNode(@PathVariable Long id) {
        var r = nodeTestService.test(id);
        return new ClientDtos.NodeTestResult(
                r.nodeId(),
                r.nodeName(),
                r.tcp().reachable(),
                r.tcp().latencyMs(),
                r.proxy().ok(),
                r.proxy().latencyMs(),
                r.proxy().speedBytesPerSec(),
                r.proxy().error(),
                r.warnings()
        );
    }

    @PostMapping("/connect")
    public ClientDtos.ConnectResponse connect(
            HttpServletRequest request,
            @RequestBody ClientDtos.ConnectRequest req
    ) {
        VpnUser user = (VpnUser) request.getAttribute(ClientAuthInterceptor.ATTR_USER);
        VpnDevice device = (VpnDevice) request.getAttribute(ClientAuthInterceptor.ATTR_DEVICE);
        return connectService.connect(user, device, req.nodeId(), request);
    }

    @PostMapping("/heartbeat")
    public ClientDtos.HeartbeatResponse heartbeat(
            HttpServletRequest request,
            @RequestBody ClientDtos.HeartbeatRequest req
    ) {
        VpnDevice device = (VpnDevice) request.getAttribute(ClientAuthInterceptor.ATTR_DEVICE);
        return connectService.heartbeat(device, req, request);
    }

    @PostMapping("/disconnect")
    public Map<String, String> disconnect(
            HttpServletRequest request,
            @RequestBody ClientDtos.DisconnectRequest req
    ) {
        VpnDevice device = (VpnDevice) request.getAttribute(ClientAuthInterceptor.ATTR_DEVICE);
        connectService.disconnect(device, req.sessionId(), req.reason());
        return Map.of("message", "ok");
    }
}
