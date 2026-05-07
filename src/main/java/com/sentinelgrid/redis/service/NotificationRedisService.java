package com.sentinelgrid.redis.service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.sentinelgrid.config.SentinelGridProperties;
import com.sentinelgrid.redis.keys.RedisKeyBuilder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationRedisService {

    private final StringRedisTemplate redisTemplate;
    private final Duration notifCooldown;

    public NotificationRedisService(StringRedisTemplate redisTemplate,
                                     SentinelGridProperties properties) {
        this.redisTemplate = redisTemplate;
        this.notifCooldown = Duration.ofMinutes(properties.getRedis().getNotificationCooldownMinutes());
    }

    public void dispatch(UUID recipientId, String notificationPayload) {
        String cooldownKey = RedisKeyBuilder.notificationCooldown(recipientId);
        Boolean isFirstInWindow = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", notifCooldown);

        if (Boolean.TRUE.equals(isFirstInWindow)) {
            log.info("NOTIFICATION_IMMEDIATE: recipientId={} payload={}", recipientId, notificationPayload);
        } else {
            String pendingKey = RedisKeyBuilder.pendingNotifications(recipientId);
            redisTemplate.opsForList().rightPush(pendingKey, notificationPayload);
            redisTemplate.opsForSet().add(RedisKeyBuilder.pendingNotifUsersSet(), recipientId.toString());
            log.debug("Notification queued for recipientId={}", recipientId);
        }
    }

    public Set<String> getPendingUserIds() {
        return redisTemplate.opsForSet().members(RedisKeyBuilder.pendingNotifUsersSet());
    }

    public List<String> drainPendingNotifications(UUID userId) {
        String listKey = RedisKeyBuilder.pendingNotifications(userId);
        List<String> notifications = redisTemplate.opsForList().range(listKey, 0, -1);
        if (notifications != null && !notifications.isEmpty()) {
            redisTemplate.delete(listKey);
        }
        return notifications == null ? List.of() : notifications;
    }

    public void removePendingUser(UUID userId) {
        redisTemplate.opsForSet().remove(RedisKeyBuilder.pendingNotifUsersSet(), userId.toString());
    }
}
