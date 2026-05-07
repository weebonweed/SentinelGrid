package com.sentinelgrid.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sentinelgrid.entity.Post;
import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.redis.service.NotificationRedisService;
import com.sentinelgrid.repository.PostRepository;
import com.sentinelgrid.service.impl.NotificationServiceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRedisService notificationRedisService;
    @Mock private PostRepository postRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void notifyPostOwner_dispatchesNotification_whenOwnerIsHuman() {
        UUID postId   = UUID.randomUUID();
        UUID actorId  = UUID.randomUUID();
        UUID ownerId  = UUID.randomUUID();

        Post post = Post.builder()
            .id(postId)
            .authorId(ownerId)
            .authorType(AuthorType.HUMAN)
            .content("post")
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        notificationService.notifyPostOwner(postId, actorId, "liked");

        verify(notificationRedisService).dispatch(eq(ownerId), anyString());
    }

    @Test
    void notifyPostOwner_skipsDispatch_whenOwnerIsBot() {
        UUID postId  = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID botId   = UUID.randomUUID();

        Post post = Post.builder()
            .id(postId)
            .authorId(botId)
            .authorType(AuthorType.BOT)
            .content("post")
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        notificationService.notifyPostOwner(postId, actorId, "commented");

        verify(notificationRedisService, never()).dispatch(eq(botId), anyString());
    }

    @Test
    void notifyPostOwner_skipsDispatch_whenActorIsOwner() {
        UUID postId  = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Post post = Post.builder()
            .id(postId)
            .authorId(ownerId)
            .authorType(AuthorType.HUMAN)
            .content("post")
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        notificationService.notifyPostOwner(postId, ownerId, "liked");

        verify(notificationRedisService, never()).dispatch(eq(ownerId), anyString());
    }

    @Test
    void notifyPostOwner_doesNotThrow_whenPostNotFound() {
        UUID postId  = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        notificationService.notifyPostOwner(postId, actorId, "commented");

        verify(notificationRedisService, never()).dispatch(eq(actorId), anyString());
    }

    @Test
    void processAllPending_drainsAndLogsForEachUser() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        when(notificationRedisService.getPendingUserIds())
            .thenReturn(Set.of(userId1.toString(), userId2.toString()));
        when(notificationRedisService.drainPendingNotifications(userId1))
            .thenReturn(List.of("notif1", "notif2"));
        when(notificationRedisService.drainPendingNotifications(userId2))
            .thenReturn(List.of("notif3"));

        notificationService.processAllPending();

        verify(notificationRedisService).drainPendingNotifications(userId1);
        verify(notificationRedisService).drainPendingNotifications(userId2);
        verify(notificationRedisService).removePendingUser(userId1);
        verify(notificationRedisService).removePendingUser(userId2);
    }

    @Test
    void processAllPending_doesNothing_whenNoPendingUsers() {
        when(notificationRedisService.getPendingUserIds()).thenReturn(Set.of());

        notificationService.processAllPending();

        verify(notificationRedisService, never()).drainPendingNotifications(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void processAllPending_skipsInvalidUserIds_gracefully() {
        when(notificationRedisService.getPendingUserIds())
            .thenReturn(Set.of("not-a-valid-uuid"));

        notificationService.processAllPending();

        verify(notificationRedisService, never()).drainPendingNotifications(org.mockito.ArgumentMatchers.any());
    }
}
