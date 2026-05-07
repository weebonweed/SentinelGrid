package com.sentinelgrid.redis.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.sentinelgrid.config.SentinelGridProperties;
import com.sentinelgrid.redis.keys.RedisKeyBuilder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CooldownRedisService {

    private final StringRedisTemplate redisTemplate;
    private final Duration cooldownDuration;

    public CooldownRedisService(StringRedisTemplate redisTemplate,
                                  SentinelGridProperties properties) {
        this.redisTemplate = redisTemplate;
        this.cooldownDuration = Duration.ofMinutes(properties.getRedis().getCooldownTtlMinutes());
    }

    public boolean tryAcquire(UUID botId, UUID humanId) {
        String key = RedisKeyBuilder.cooldown(botId, humanId);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", cooldownDuration);
        boolean result = Boolean.TRUE.equals(acquired);
        if (!result) {
            log.warn("Cooldown active: botId={} humanId={}", botId, humanId);
        }
        return result;
    }

    public void release(UUID botId, UUID humanId) {
        redisTemplate.delete(RedisKeyBuilder.cooldown(botId, humanId));
        log.debug("Cooldown released: botId={} humanId={}", botId, humanId);
    }

    public boolean isActive(UUID botId, UUID humanId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeyBuilder.cooldown(botId, humanId)));
    }
}
