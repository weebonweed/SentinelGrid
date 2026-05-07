package com.sentinelgrid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "sentinelgrid")
public class SentinelGridProperties {

    private final Redis redis = new Redis();
    private final Scheduler scheduler = new Scheduler();
    private final Security security = new Security();

    @Data
    public static class Redis {
        private int botCap = 100;
        private int cooldownTtlMinutes = 10;
        private int notificationCooldownMinutes = 15;
    }

    @Data
    public static class Scheduler {
        private String notificationSweeperCron = "0 */5 * * * *";
    }

    @Data
    public static class Security {
        private long maxPayloadBytes = 10240L;
    }
}
