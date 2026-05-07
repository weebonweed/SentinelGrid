package com.sentinelgrid.redis.keys;

import java.util.UUID;

public final class RedisKeyBuilder {

    private static final String POST_VIRALITY_SCORE   = "post:%s:virality_score";
    private static final String POST_BOT_COUNT        = "post:%s:bot_count";
    private static final String COOLDOWN_BOT_HUMAN    = "cooldown:bot_%s:human_%s";
    private static final String USER_PENDING_NOTIFS   = "user:%s:pending_notifs";
    private static final String USER_NOTIF_COOLDOWN   = "user:%s:notif_cooldown";
    private static final String PENDING_NOTIF_USERS   = "system:pending_notif_users";

    private RedisKeyBuilder() {}

    public static String viralityScore(UUID postId) {
        return String.format(POST_VIRALITY_SCORE, postId);
    }

    public static String botCount(UUID postId) {
        return String.format(POST_BOT_COUNT, postId);
    }

    public static String cooldown(UUID botId, UUID humanId) {
        return String.format(COOLDOWN_BOT_HUMAN, botId, humanId);
    }

    public static String pendingNotifications(UUID userId) {
        return String.format(USER_PENDING_NOTIFS, userId);
    }

    public static String notificationCooldown(UUID userId) {
        return String.format(USER_NOTIF_COOLDOWN, userId);
    }

    public static String pendingNotifUsersSet() {
        return PENDING_NOTIF_USERS;
    }
}
