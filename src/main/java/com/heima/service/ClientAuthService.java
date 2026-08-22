package com.heima.service;

import com.heima.config.MyvpnProperties;
import com.heima.dto.ClientDtos;
import com.heima.entity.*;
import com.heima.mapper.*;
import com.heima.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClientAuthService {

    private final VpnUserMapper userMapper;
    private final VpnDeviceMapper deviceMapper;
    private final VpnUserSessionMapper sessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final MyvpnProperties props;

    @Transactional
    public ClientDtos.AuthResponse register(ClientDtos.RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (email.isEmpty()) throw new IllegalArgumentException("请填写邮箱");
        if (req.password() == null || req.password().length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (userMapper.findByEmail(email) != null) {
            throw new IllegalArgumentException("邮箱已注册");
        }
        VpnUser user = new VpnUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank() ? email : req.nickname().trim());
        userMapper.insert(user);
        return new ClientDtos.AuthResponse("", email, user.getNickname(), null, "");
    }

    @Transactional
    public ClientDtos.AuthResponse login(ClientDtos.LoginRequest req, HttpServletRequest request) {
        String email = normalizeEmail(req.email());
        VpnUser user = userMapper.findByEmail(email);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("邮箱或密码错误");
        }
        if (user.getEnabled() != null && user.getEnabled() == 0) {
            throw new IllegalArgumentException("账号已禁用");
        }
        if (user.getPlanExpireAt() != null && user.getPlanExpireAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("套餐已过期");
        }
        VpnDevice device = ensureDevice(user, req);
        String token = createToken(user.getId(), device.getId());
        userMapper.updateLastLogin(user.getId());
        deviceMapper.updateLastSeen(device.getId(), ClientIp.resolve(request));
        return new ClientDtos.AuthResponse(token, user.getEmail(), user.getNickname(), device.getId(), device.getV2rayUuid());
    }

    public VpnUser findUserByToken(String token) {
        VpnUserSession session = sessionMapper.findByToken(token);
        if (session == null) return null;
        VpnUser user = userMapper.findById(session.getUserId());
        if (user == null || user.getEnabled() == 0) return null;
        return user;
    }

    public VpnDevice findDeviceByToken(String token) {
        VpnUserSession session = sessionMapper.findByToken(token);
        if (session == null) return null;
        return deviceMapper.findById(session.getDeviceId());
    }

    public void logout(String token) {
        sessionMapper.deleteByToken(token);
    }

    private VpnDevice ensureDevice(VpnUser user, ClientDtos.LoginRequest req) {
        String uuid = req.deviceUuid() == null ? "" : req.deviceUuid().trim();
        if (uuid.isEmpty()) throw new IllegalArgumentException("缺少设备标识");
        VpnDevice device = deviceMapper.findByUserAndUuid(user.getId(), uuid);
        if (device != null) {
            if (device.getEnabled() == 0) throw new IllegalArgumentException("设备已禁用");
            return device;
        }
        device = new VpnDevice();
        device.setUserId(user.getId());
        device.setDeviceUuid(uuid);
        device.setDeviceName(req.deviceName() == null ? "未命名设备" : req.deviceName().trim());
        device.setPlatform(req.platform() == null ? "unknown" : req.platform().trim());
        device.setV2rayUuid(UUID.randomUUID().toString());
        deviceMapper.insert(device);
        return device;
    }

    private String createToken(Long userId, Long deviceId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        VpnUserSession session = new VpnUserSession();
        session.setUserId(userId);
        session.setDeviceId(deviceId);
        session.setToken(token);
        session.setExpireAt(LocalDateTime.now().plusDays(props.getClient().getTokenDays()));
        sessionMapper.insert(session);
        return token;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
