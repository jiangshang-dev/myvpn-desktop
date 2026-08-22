package com.heima.web;

import com.heima.dto.ApiError;
import com.heima.entity.VpnDevice;
import com.heima.entity.VpnUser;
import com.heima.service.ClientAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ClientAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER = "vpnUser";
    public static final String ATTR_DEVICE = "vpnDevice";

    private final ClientAuthService clientAuthService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (request.getRequestURI().contains("/health")) return true;
        String token = bearer(request);
        VpnUser user = token == null ? null : clientAuthService.findUserByToken(token);
        if (user == null) {
            write(response, 401, "请先登录");
            return false;
        }
        VpnDevice device = clientAuthService.findDeviceByToken(token);
        if (device == null || device.getEnabled() == 0) {
            write(response, 403, "设备不可用");
            return false;
        }
        request.setAttribute(ATTR_USER, user);
        request.setAttribute(ATTR_DEVICE, device);
        return true;
    }

    private String bearer(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) return null;
        return h.substring(7).trim();
    }

    private void write(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ApiError(msg)));
    }
}
