package com.heima.service;

import com.heima.config.MyvpnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VpnRedisService {

    private final StringRedisTemplate redis;
    private final MyvpnProperties props;

    private String p(String key) {
        return props.getSession().getRedisPrefix() + key;
    }

    public void touchSession(long sessionId, long uploadSpeed, long downloadSpeed) {
        String key = p("online:" + sessionId);
        redis.opsForHash().put(key, "uploadSpeed", String.valueOf(uploadSpeed));
        redis.opsForHash().put(key, "downloadSpeed", String.valueOf(downloadSpeed));
        redis.opsForHash().put(key, "lastBeat", String.valueOf(System.currentTimeMillis()));
        redis.expire(key, props.getClient().getHeartbeatTimeoutSeconds() * 2L, TimeUnit.SECONDS);
        redis.opsForSet().add(p("online:all"), String.valueOf(sessionId));
    }

    public void removeSession(long sessionId) {
        redis.delete(p("online:" + sessionId));
        redis.opsForSet().remove(p("online:all"), String.valueOf(sessionId));
    }

    public void kickDevice(long deviceId) {
        redis.opsForValue().set(p("cmd:" + deviceId), "KICK", Duration.ofMinutes(10));
    }

    public String pollCommand(long deviceId) {
        String key = p("cmd:" + deviceId);
        String cmd = redis.opsForValue().get(key);
        if (cmd != null) {
            redis.delete(key);
        }
        return cmd;
    }

    public void cacheNodeOnline(long nodeId, long count) {
        redis.opsForValue().set(p("node:count:" + nodeId), String.valueOf(count), Duration.ofMinutes(5));
    }

    public long getCachedNodeOnline(long nodeId) {
        String v = redis.opsForValue().get(p("node:count:" + nodeId));
        return v == null ? 0 : Long.parseLong(v);
    }
}
