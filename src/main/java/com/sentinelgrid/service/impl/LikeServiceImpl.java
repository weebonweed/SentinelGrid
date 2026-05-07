package com.sentinelgrid.service.impl;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.sentinelgrid.service.interfaces.LikeService;
import com.sentinelgrid.service.interfaces.NotificationService;
import com.sentinelgrid.util.AuthorResolver;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LikeServiceImpl implements LikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final AuthorResolver authorResolver;
    private final ViralityRedisService viralityRedisService;
    private final NotificationService notificationService;
    private final EntityMapper entityMapper;

    public LikeServiceImpl(PostRepository postRepository,
                            PostLikeRepository postLikeRepository,
                            AuthorResolver authorResolver,
                            ViralityRedisService viralityRedisService,
                            NotificationService notificationService,
                            EntityMapper entityMapper) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.authorResolver = authorResolver;
        this.viralityRedisService = viralityRedisService;
        this.notificationService = notificationService;
        this.entityMapper = entityMapper;
    }

    @Override
    @Transactional
    public LikeResponse likePost(UUID postId, LikePostRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        authorResolver.assertExists(request.getAuthorId(), request.getAuthorType());

        if (postLikeRepository.existsByPostIdAndAuthorIdAndAuthorType(
                postId, request.getAuthorId(), request.getAuthorType())) {
            throw new DataIntegrityViolationException(
                "Author " + request.getAuthorId() + " already liked post " + postId);
        }

        PostLike like = PostLike.builder()
            .post(post)
            .authorId(request.getAuthorId())
            .authorType(request.getAuthorType())
            .build();

        PostLike saved = postLikeRepository.save(like);

        if (request.getAuthorType() == AuthorType.HUMAN) {
            viralityRedisService.addHumanLike(postId);
            notificationService.notifyPostOwner(postId, request.getAuthorId(), "liked");
        }

        log.info("Post liked: postId={} authorId={} authorType={}",
            postId, request.getAuthorId(), request.getAuthorType());

        return entityMapper.toLikeResponse(saved);
    }
}
