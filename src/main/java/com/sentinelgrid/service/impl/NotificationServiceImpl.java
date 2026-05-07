package com.sentinelgrid.service.impl;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sentinelgrid.entity.Post;
import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.redis.service.NotificationRedisService;
import com.sentinelgrid.repository.PostRepository;
import com.sentinelgrid.service.interfaces.NotificationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRedisService notificationRedisService;
    private final PostRepository postRepository;

    public NotificationServiceImpl(NotificationRedisService notificationRedisService,
                                    PostRepository postRepository) {
        this.notificationRedisService = notificationRedisService;
        this.postRepository = postRepository;
    }

    @Override
    public void notifyPostOwner(UUID postId, UUID actorId, String action) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            log.warn("Cannot notify: post not found postId={}", postId);
            return;
        }

        if (post.getAuthorType() != AuthorType.HUMAN) {
            return;
        }

        if (post.getAuthorId().equals(actorId)) {
            return;
        }

        UUID recipientId = post.getAuthorId();
        String payload = buildPayload(postId, actorId, action);
        notificationRedisService.dispatch(recipientId, payload);
    }

    @Override
    public void processAllPending() {
        Set<String> pendingUserIds = notificationRedisService.getPendingUserIds();
        if (pendingUserIds == null || pendingUserIds.isEmpty()) {
            log.debug("No pending notifications to process");
            return;
        }

        for (String userIdStr : pendingUserIds) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                processPendingForUser(userId);
            } catch (IllegalArgumentException e) {
                log.error("Invalid userId in pending set: {}", userIdStr);
            }
        }
    }

    private void processPendingForUser(UUID userId) {
        List<String> notifications = notificationRedisService.drainPendingNotifications(userId);
        if (notifications.isEmpty()) {
            notificationRedisService.removePendingUser(userId);
            return;
        }

        log.info("NOTIFICATION_BATCH: recipientId={} count={} summary={}",
            userId, notifications.size(), buildSummary(notifications));

        notificationRedisService.removePendingUser(userId);
    }

    private String buildPayload(UUID postId, UUID actorId, String action) {
        return String.format("actor=%s action=%s postId=%s", actorId, action, postId);
    }

    private String buildSummary(List<String> notifications) {
        return "You have " + notifications.size() + " new notification(s): "
            + String.join("; ", notifications.subList(0, Math.min(3, notifications.size())))
            + (notifications.size() > 3 ? " ..." : "");
    }
}
