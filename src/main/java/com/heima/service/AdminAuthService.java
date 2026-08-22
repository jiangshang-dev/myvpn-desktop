package com.heima.service;

import com.heima.config.MyvpnProperties;
import com.heima.entity.VpnAdmin;
import com.heima.entity.VpnAdminSession;
import com.heima.mapper.VpnAdminMapper;
import com.heima.mapper.VpnAdminSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final VpnAdminMapper adminMapper;
    private final VpnAdminSessionMapper sessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final MyvpnProperties props;

    public VpnAdmin login(String account, String password) {
        VpnAdmin admin = adminMapper.findByAccount(account);
        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return admin;
    }

    public String createSession(VpnAdmin admin) {
        String token = UUID.randomUUID().toString().replace("-", "");
        VpnAdminSession session = new VpnAdminSession();
        session.setAdminId(admin.getId());
        session.setToken(token);
        session.setExpireAt(LocalDateTime.now().plusDays(props.getAdmin().getTokenDays()));
        sessionMapper.insert(session);
        return token;
    }

    public VpnAdmin findByToken(String token) {
        VpnAdminSession session = sessionMapper.findByToken(token);
        if (session == null) return null;
        return adminMapper.findById(session.getAdminId());
    }

    public void logout(String token) {
        sessionMapper.deleteByToken(token);
    }

    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        VpnAdmin admin = adminMapper.findById(adminId);
        if (admin == null || !passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        adminMapper.updatePassword(adminId, passwordEncoder.encode(newPassword));
    }
}
