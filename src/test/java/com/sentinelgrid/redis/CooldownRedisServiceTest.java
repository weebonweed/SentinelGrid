package com.sentinelgrid.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.sentinelgrid.config.SentinelGridProperties;
import com.sentinelgrid.redis.service.CooldownRedisService;

@ExtendWith(MockitoExtension.class)
class CooldownRedisServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private CooldownRedisService cooldownRedisService;

    @BeforeEach
    void setUp() {
        SentinelGridProperties properties = new SentinelGridProperties();
        properties.getRedis().setCooldownTtlMinutes(10);
        cooldownRedisService = new CooldownRedisService(redisTemplate, properties);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void tryAcquire_returnsTrue_whenNoExistingCooldown() {
        UUID botId = UUID.randomUUID();
        UUID humanId = UUID.randomUUID();
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        assertThat(cooldownRedisService.tryAcquire(botId, humanId)).isTrue();
    }

    @Test
    void tryAcquire_returnsFalse_whenCooldownAlreadyActive() {
        UUID botId = UUID.randomUUID();
        UUID humanId = UUID.randomUUID();
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        assertThat(cooldownRedisService.tryAcquire(botId, humanId)).isFalse();
    }

    @Test
    void tryAcquire_returnsFalse_whenRedisReturnsNull() {
        UUID botId = UUID.randomUUID();
        UUID humanId = UUID.randomUUID();
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(null);

        assertThat(cooldownRedisService.tryAcquire(botId, humanId)).isFalse();
    }

    @Test
    void isActive_returnTrue_whenKeyExists() {
        UUID botId = UUID.randomUUID();
        UUID humanId = UUID.randomUUID();
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertThat(cooldownRedisService.isActive(botId, humanId)).isTrue();
    }

    @Test
    void isActive_returnsFalse_whenKeyAbsent() {
        UUID botId = UUID.randomUUID();
        UUID humanId = UUID.randomUUID();
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertThat(cooldownRedisService.isActive(botId, humanId)).isFalse();
    }

    @Test
    void release_deletesCorrectKey() {
        UUID botId = UUID.randomUUID();
        UUID humanId = UUID.randomUUID();
        String expectedKey = "cooldown:bot_" + botId + ":human_" + humanId;

        cooldownRedisService.release(botId, humanId);

        verify(redisTemplate).delete(expectedKey);
    }
}
