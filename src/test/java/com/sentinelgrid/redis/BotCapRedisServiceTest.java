package com.sentinelgrid.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.sentinelgrid.config.SentinelGridProperties;
import com.sentinelgrid.redis.service.BotCapRedisService;

@ExtendWith(MockitoExtension.class)
class BotCapRedisServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private DefaultRedisScript<Long> botCapScript;
    @Mock private ValueOperations<String, String> valueOps;

    private BotCapRedisService botCapRedisService;

    @BeforeEach
    void setUp() {
        SentinelGridProperties properties = new SentinelGridProperties();
        properties.getRedis().setBotCap(100);
        botCapRedisService = new BotCapRedisService(redisTemplate, botCapScript, properties);
    }

    @Test
    void tryReserve_returnsTrue_whenScriptReturnsOne() {
        UUID postId = UUID.randomUUID();
        when(redisTemplate.execute(
            eq(botCapScript),
            any(),
            any(String.class)
        )).thenReturn(1L);

        boolean result = botCapRedisService.tryReserve(postId);
        assertThat(result).isTrue();
    }

    @Test
    void tryReserve_returnsFalse_whenScriptReturnsZero() {
        UUID postId = UUID.randomUUID();
        when(redisTemplate.execute(
            eq(botCapScript),
            any(),
            any(String.class)
        )).thenReturn(0L);

        boolean result = botCapRedisService.tryReserve(postId);
        assertThat(result).isFalse();
    }

    @Test
    void tryReserve_returnsFalse_whenScriptReturnsNull() {
        UUID postId = UUID.randomUUID();
        when(redisTemplate.execute(
            eq(botCapScript),
            any(),
            any(String.class)
        )).thenReturn(null);

        boolean result = botCapRedisService.tryReserve(postId);
        assertThat(result).isFalse();
    }

    @Test
    void getCurrentCount_returnsZero_whenKeyAbsent() {
        UUID postId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        long count = botCapRedisService.getCurrentCount(postId);
        assertThat(count).isZero();
    }

    @Test
    void getCurrentCount_returnsParsedValue_whenKeyPresent() {
        UUID postId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("57");

        long count = botCapRedisService.getCurrentCount(postId);
        assertThat(count).isEqualTo(57L);
    }

    @Test
    void release_decrementsKey() {
        UUID postId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.decrement(anyString())).thenReturn(42L);

        botCapRedisService.release(postId);
        verify(valueOps).decrement(anyString());
    }

    @Test
    void release_resetsToZero_whenCountGoesNegative() {
        UUID postId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.decrement(anyString())).thenReturn(-1L);

        botCapRedisService.release(postId);
        verify(valueOps).set(anyString(), eq("0"));
    }
}
