package com.sentinelgrid.redis.service;

import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.sentinelgrid.redis.keys.RedisKeyBuilder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ViralityRedisService {

    private static final int BOT_REPLY_POINTS    = 1;
    private static final int HUMAN_LIKE_POINTS   = 20;
    private static final int HUMAN_COMMENT_POINTS = 50;

    private final StringRedisTemplate redisTemplate;

    public ViralityRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long addBotReply(UUID postId) {
        return increment(postId, BOT_REPLY_POINTS);
    }

    public long addHumanLike(UUID postId) {
        return increment(postId, HUMAN_LIKE_POINTS);
    }

    public long addHumanComment(UUID postId) {
        return increment(postId, HUMAN_COMMENT_POINTS);
    }

    public long getScore(UUID postId) {
        String value = redisTemplate.opsForValue().get(RedisKeyBuilder.viralityScore(postId));
        return value == null ? 0L : Long.parseLong(value);
    }

    private long increment(UUID postId, int points) {
        String key = RedisKeyBuilder.viralityScore(postId);
        Long result = redisTemplate.opsForValue().increment(key, points);
        long score = result == null ? 0L : result;
        log.debug("Virality updated: postId={} delta={} newScore={}", postId, points, score);
        return score;
    }
}
