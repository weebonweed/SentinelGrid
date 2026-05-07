package com.sentinelgrid.redis.service;

import java.util.Collections;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.sentinelgrid.config.SentinelGridProperties;
import com.sentinelgrid.redis.keys.RedisKeyBuilder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BotCapRedisService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> botCapScript;
    private final int botCap;

    public BotCapRedisService(StringRedisTemplate redisTemplate,
                               DefaultRedisScript<Long> botCapScript,
                               SentinelGridProperties properties) {
        this.redisTemplate = redisTemplate;
        this.botCapScript = botCapScript;
        this.botCap = properties.getRedis().getBotCap();
    }

    public boolean tryReserve(UUID postId) {
        String key = RedisKeyBuilder.botCount(postId);
        Long result = redisTemplate.execute(
            botCapScript,
            Collections.singletonList(key),
            String.valueOf(botCap)
        );
        boolean reserved = Long.valueOf(1L).equals(result);
        if (!reserved) {
            log.warn("Bot cap reached for postId={} cap={}", postId, botCap);
        }
        return reserved;
    }

    public void release(UUID postId) {
        String key = RedisKeyBuilder.botCount(postId);
        Long current = redisTemplate.opsForValue().decrement(key);
        if (current != null && current < 0) {
            redisTemplate.opsForValue().set(key, "0");
            log.warn("Bot count went negative for postId={}, reset to 0", postId);
        }
        log.debug("Released bot cap reservation for postId={}", postId);
    }

    public long getCurrentCount(UUID postId) {
        String value = redisTemplate.opsForValue().get(RedisKeyBuilder.botCount(postId));
        return value == null ? 0L : Long.parseLong(value);
    }
}
