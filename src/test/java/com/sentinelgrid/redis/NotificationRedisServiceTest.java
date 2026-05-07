package com.sentinelgrid.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.sentinelgrid.config.SentinelGridProperties;
import com.sentinelgrid.redis.service.NotificationRedisService;

@ExtendWith(MockitoExtension.class)
class NotificationRedisServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ListOperations<String, String> listOps;
    @Mock private SetOperations<String, String> setOps;

    private NotificationRedisService notificationRedisService;

    @BeforeEach
    void setUp() {
        SentinelGridProperties properties = new SentinelGridProperties();
        properties.getRedis().setNotificationCooldownMinutes(15);
        notificationRedisService = new NotificationRedisService(redisTemplate, properties);
    }

    @Test
    void dispatch_logsImmediately_whenCooldownNotActive() {
        UUID recipientId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        notificationRedisService.dispatch(recipientId, "actor=x action=liked postId=y");

        verify(valueOps).setIfAbsent(anyString(), eq("1"), any(Duration.class));
        verify(redisTemplate, never()).opsForList();
    }

    @Test
    void dispatch_queuesInList_whenCooldownAlreadyActive() {
        UUID recipientId = UUID.randomUUID();
        String payload = "actor=x action=commented postId=y";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        notificationRedisService.dispatch(recipientId, payload);

        verify(listOps).rightPush(anyString(), eq(payload));
        verify(setOps).add(anyString(), eq(recipientId.toString()));
    }

    @Test
    void drainPendingNotifications_returnsEmptyList_whenNoPending() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(List.of());

        List<String> result = notificationRedisService.drainPendingNotifications(userId);

        assertThat(result).isEmpty();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void drainPendingNotifications_deletesKey_afterDraining() {
        UUID userId = UUID.randomUUID();
        List<String> notifications = List.of("notif1", "notif2");

        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(notifications);

        List<String> result = notificationRedisService.drainPendingNotifications(userId);

        assertThat(result).hasSize(2);
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void removePendingUser_removesFromSet() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        notificationRedisService.removePendingUser(userId);

        verify(setOps).remove(anyString(), eq(userId.toString()));
    }
}
