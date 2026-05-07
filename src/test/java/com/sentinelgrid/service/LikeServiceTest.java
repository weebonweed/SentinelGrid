package com.sentinelgrid.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sentinelgrid.dto.request.LikePostRequest;
import com.sentinelgrid.dto.response.LikeResponse;
import com.sentinelgrid.entity.Post;
import com.sentinelgrid.entity.PostLike;
import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.exception.ResourceNotFoundException;
import com.sentinelgrid.mapper.EntityMapper;
import com.sentinelgrid.redis.service.ViralityRedisService;
import com.sentinelgrid.repository.PostLikeRepository;
import com.sentinelgrid.repository.PostRepository;
import com.sentinelgrid.service.impl.LikeServiceImpl;
import com.sentinelgrid.service.interfaces.NotificationService;
import com.sentinelgrid.util.AuthorResolver;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private AuthorResolver authorResolver;
    @Mock private ViralityRedisService viralityRedisService;
    @Mock private NotificationService notificationService;
    @Mock private EntityMapper entityMapper;

    @InjectMocks
    private LikeServiceImpl likeService;

    @Test
    void likePost_humanAuthor_updatesVirality() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Post post = Post.builder()
            .id(postId)
            .authorId(ownerId)
            .authorType(AuthorType.HUMAN)
            .content("post")
            .build();

        PostLike savedLike = PostLike.builder()
            .id(UUID.randomUUID())
            .post(post)
            .authorId(authorId)
            .authorType(AuthorType.HUMAN)
            .build();

        LikeResponse expectedResponse = LikeResponse.builder()
            .likeId(savedLike.getId())
            .postId(postId)
            .authorId(authorId)
            .authorType(AuthorType.HUMAN)
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByPostIdAndAuthorIdAndAuthorType(postId, authorId, AuthorType.HUMAN))
            .thenReturn(false);
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(savedLike);
        when(entityMapper.toLikeResponse(savedLike)).thenReturn(expectedResponse);

        LikeResponse result = likeService.likePost(postId, new LikePostRequest(authorId, AuthorType.HUMAN));

        assertThat(result).isNotNull();
        verify(viralityRedisService).addHumanLike(postId);
    }

    @Test
    void likePost_duplicateLike_throwsConflict() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Post post = Post.builder()
            .id(postId)
            .authorId(UUID.randomUUID())
            .authorType(AuthorType.HUMAN)
            .content("post")
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByPostIdAndAuthorIdAndAuthorType(postId, authorId, AuthorType.HUMAN))
            .thenReturn(true);

        assertThatThrownBy(() ->
            likeService.likePost(postId, new LikePostRequest(authorId, AuthorType.HUMAN)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void likePost_postNotFound_throwsResourceNotFoundException() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            likeService.likePost(postId, new LikePostRequest(UUID.randomUUID(), AuthorType.HUMAN)))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
