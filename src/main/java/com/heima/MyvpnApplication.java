package com.heima;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.heima.mapper")
@ConfigurationPropertiesScan
@EnableScheduling
public class MyvpnApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyvpnApplication.class, args);
    }
}
