package com.sentinelgrid.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.sentinelgrid.redis.service.ViralityRedisService;

@ExtendWith(MockitoExtension.class)
class ViralityRedisServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private ViralityRedisService viralityRedisService;

    @BeforeEach
    void setUp() {
        viralityRedisService = new ViralityRedisService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void addBotReply_returnsNewScore_andIncrementsBy1() {
        UUID postId = UUID.randomUUID();
        when(valueOps.increment(anyString(), anyLong())).thenReturn(1L);

        long result = viralityRedisService.addBotReply(postId);

        assertThat(result).isEqualTo(1L);
        verify(valueOps).increment(anyString(), anyLong());
    }

    @Test
    void addHumanLike_returnsNewScore_andIncrementsBy20() {
        UUID postId = UUID.randomUUID();
        when(valueOps.increment(anyString(), anyLong())).thenReturn(20L);

        long result = viralityRedisService.addHumanLike(postId);

        assertThat(result).isEqualTo(20L);
    }

    @Test
    void addHumanComment_returnsNewScore_andIncrementsBy50() {
        UUID postId = UUID.randomUUID();
        when(valueOps.increment(anyString(), anyLong())).thenReturn(50L);

        long result = viralityRedisService.addHumanComment(postId);

        assertThat(result).isEqualTo(50L);
    }

    @Test
    void getScore_returnsZero_whenKeyAbsent() {
        UUID postId = UUID.randomUUID();
        when(valueOps.get(anyString())).thenReturn(null);

        long score = viralityRedisService.getScore(postId);

        assertThat(score).isZero();
    }

    @Test
    void getScore_returnsCorrectValue_whenKeyPresent() {
        UUID postId = UUID.randomUUID();
        when(valueOps.get(anyString())).thenReturn("142");

        long score = viralityRedisService.getScore(postId);

        assertThat(score).isEqualTo(142L);
    }

    @Test
    void addBotReply_returnsZero_whenRedisReturnsNull() {
        UUID postId = UUID.randomUUID();
        when(valueOps.increment(anyString(), anyLong())).thenReturn(null);

        long result = viralityRedisService.addBotReply(postId);

        assertThat(result).isZero();
    }
}
