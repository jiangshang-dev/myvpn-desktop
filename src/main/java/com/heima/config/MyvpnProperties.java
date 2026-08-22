package com.heima.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "myvpn")
public class MyvpnProperties {

    private Admin admin = new Admin();
    private Client client = new Client();
    private Session session = new Session();

    @Data
    public static class Admin {
        private int tokenDays = 7;
    }

    @Data
    public static class Client {
        private int tokenDays = 30;
        private int heartbeatIntervalSeconds = 15;
        private int heartbeatTimeoutSeconds = 60;
    }

    @Data
    public static class Session {
        private String redisPrefix = "vpn:";
    }
}
