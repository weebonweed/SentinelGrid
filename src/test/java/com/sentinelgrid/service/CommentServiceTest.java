package com.sentinelgrid.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sentinelgrid.dto.request.CreateCommentRequest;
import com.sentinelgrid.dto.response.CommentResponse;
import com.sentinelgrid.entity.Comment;
import com.sentinelgrid.entity.Post;
import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.exception.CooldownViolationException;
import com.sentinelgrid.exception.DepthLimitExceededException;
import com.sentinelgrid.exception.RateLimitExceededException;
import com.sentinelgrid.exception.ResourceNotFoundException;
import com.sentinelgrid.mapper.EntityMapper;
import com.sentinelgrid.redis.service.BotCapRedisService;
import com.sentinelgrid.redis.service.CooldownRedisService;
import com.sentinelgrid.redis.service.ViralityRedisService;
import com.sentinelgrid.repository.CommentRepository;
import com.sentinelgrid.repository.PostRepository;
import com.sentinelgrid.service.impl.CommentServiceImpl;
import com.sentinelgrid.service.interfaces.NotificationService;
import com.sentinelgrid.util.AuthorResolver;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private AuthorResolver authorResolver;
    @Mock private BotCapRedisService botCapRedisService;
    @Mock private CooldownRedisService cooldownRedisService;
    @Mock private ViralityRedisService viralityRedisService;
    @Mock private NotificationService notificationService;
    @Mock private EntityMapper entityMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void createComment_humanAuthor_succeeds() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Post post = Post.builder()
            .id(postId)
            .authorId(UUID.randomUUID())
            .authorType(AuthorType.HUMAN)
            .content("post content")
            .build();

        CreateCommentRequest request = new CreateCommentRequest(
            null, authorId, AuthorType.HUMAN, "human comment");

        Comment savedComment = Comment.builder()
            .id(UUID.randomUUID())
            .post(post)
            .authorId(authorId)
            .authorType(AuthorType.HUMAN)
            .content("human comment")
            .depthLevel(0)
            .build();

        CommentResponse expectedResponse = CommentResponse.builder()
            .id(savedComment.getId())
            .postId(postId)
            .authorId(authorId)
            .authorType(AuthorType.HUMAN)
            .content("human comment")
            .depthLevel(0)
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(entityMapper.toCommentResponse(savedComment)).thenReturn(expectedResponse);

        CommentResponse result = commentService.createComment(postId, request);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorType()).isEqualTo(AuthorType.HUMAN);
        verify(botCapRedisService, never()).tryReserve(any());
        verify(viralityRedisService).addHumanComment(postId);
    }

    @Test
    void createComment_botAuthor_capExceeded_throws429() {
        UUID postId = UUID.randomUUID();
        UUID botId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Post post = Post.builder()
            .id(postId)
            .authorId(ownerId)
            .authorType(AuthorType.HUMAN)
            .content("post")
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(botCapRedisService.tryReserve(postId)).thenReturn(false);

        CreateCommentRequest request = new CreateCommentRequest(
            null, botId, AuthorType.BOT, "bot reply");

        assertThatThrownBy(() -> commentService.createComment(postId, request))
            .isInstanceOf(RateLimitExceededException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createComment_botAuthor_cooldownActive_throws429AndReleasesReservation() {
        UUID postId = UUID.randomUUID();
        UUID botId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Post post = Post.builder()
            .id(postId)
            .authorId(ownerId)
            .authorType(AuthorType.HUMAN)
            .content("post")
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(botCapRedisService.tryReserve(postId)).thenReturn(true);
        when(cooldownRedisService.tryAcquire(botId, ownerId)).thenReturn(false);

        CreateCommentRequest request = new CreateCommentRequest(
            null, botId, AuthorType.BOT, "bot reply");

        assertThatThrownBy(() -> commentService.createComment(postId, request))
            .isInstanceOf(CooldownViolationException.class);

        verify(botCapRedisService).release(postId);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createComment_depthExceeded_throwsDepthLimitException() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Post post = Post.builder()
            .id(postId)
            .authorId(UUID.randomUUID())
            .authorType(AuthorType.HUMAN)
            .content("post")
            .build();

        UUID parentId = UUID.randomUUID();
        Comment deepParent = Comment.builder()
            .id(parentId)
            .post(post)
            .authorId(UUID.randomUUID())
            .authorType(AuthorType.HUMAN)
            .content("deep comment")
            .depthLevel(20)
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(deepParent));

        CreateCommentRequest request = new CreateCommentRequest(
            parentId, authorId, AuthorType.HUMAN, "reply at depth 21");

        assertThatThrownBy(() -> commentService.createComment(postId, request))
            .isInstanceOf(DepthLimitExceededException.class);
    }

    @Test
    void createComment_postNotFound_throwsResourceNotFoundException() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        CreateCommentRequest request = new CreateCommentRequest(
            null, UUID.randomUUID(), AuthorType.HUMAN, "comment");

        assertThatThrownBy(() -> commentService.createComment(postId, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(postId.toString());
    }
}
