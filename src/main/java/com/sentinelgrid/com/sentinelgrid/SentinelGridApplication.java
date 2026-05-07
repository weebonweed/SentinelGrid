package com.sentinelgrid.com.sentinelgrid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.sentinelgrid.config.SentinelGridProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SentinelGridProperties.class)
public class SentinelGridApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelGridApplication.class, args);
    }
}
