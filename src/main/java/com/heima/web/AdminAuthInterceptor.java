package com.heima.web;

import com.heima.entity.VpnAdmin;
import com.heima.service.AdminAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ADMIN = "vpnAdmin";

    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String token = bearer(request);
        VpnAdmin admin = token == null ? null : adminAuthService.findByToken(token);
        if (admin == null) {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(new ApiError("请先登录")));
            return false;
        }
        request.setAttribute(ATTR_ADMIN, admin);
        return true;
    }

    private String bearer(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) return null;
        return h.substring(7).trim();
    }
}
