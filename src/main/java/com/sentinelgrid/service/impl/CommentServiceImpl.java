package com.sentinelgrid.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.sentinelgrid.service.interfaces.CommentService;
import com.sentinelgrid.service.interfaces.NotificationService;
import com.sentinelgrid.util.AuthorResolver;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    private static final int MAX_DEPTH = 20;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AuthorResolver authorResolver;
    private final BotCapRedisService botCapRedisService;
    private final CooldownRedisService cooldownRedisService;
    private final ViralityRedisService viralityRedisService;
    private final NotificationService notificationService;
    private final EntityMapper entityMapper;

    public CommentServiceImpl(PostRepository postRepository,
                               CommentRepository commentRepository,
                               AuthorResolver authorResolver,
                               BotCapRedisService botCapRedisService,
                               CooldownRedisService cooldownRedisService,
                               ViralityRedisService viralityRedisService,
                               NotificationService notificationService,
                               EntityMapper entityMapper) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.authorResolver = authorResolver;
        this.botCapRedisService = botCapRedisService;
        this.cooldownRedisService = cooldownRedisService;
        this.viralityRedisService = viralityRedisService;
        this.notificationService = notificationService;
        this.entityMapper = entityMapper;
    }

    @Override
    @Transactional
    public CommentResponse createComment(UUID postId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        authorResolver.assertExists(request.getAuthorId(), request.getAuthorType());

        Comment parentComment = resolveParent(request, postId);
        int depthLevel = computeDepth(parentComment);

        if (depthLevel > MAX_DEPTH) {
            throw new DepthLimitExceededException(
                "Comment thread depth cannot exceed " + MAX_DEPTH);
        }

        boolean isBotAuthor = request.getAuthorType() == AuthorType.BOT;

        if (isBotAuthor) {
            handleBotGuardrails(postId, post, request.getAuthorId());
        }

        Comment comment;
        try {
            comment = persistComment(post, parentComment, request, depthLevel);
        } catch (Exception e) {
            if (isBotAuthor) {
                botCapRedisService.release(postId);
                log.warn("DB write failed for bot comment, releasing reservation: postId={}", postId);
            }
            throw e;
        }

        updateVirality(postId, request.getAuthorType());

        notificationService.notifyPostOwner(postId, request.getAuthorId(), "commented");

        log.info("Comment created: id={} postId={} authorType={} depth={}",
            comment.getId(), postId, request.getAuthorType(), depthLevel);

        return entityMapper.toCommentResponse(comment);
    }

    private void handleBotGuardrails(UUID postId, Post post, UUID botAuthorId) {
        boolean reserved = botCapRedisService.tryReserve(postId);
        if (!reserved) {
            throw new RateLimitExceededException(
                "Bot reply cap reached for postId: " + postId);
        }

        if (post.getAuthorType() == AuthorType.HUMAN) {
            boolean cooldownAcquired = cooldownRedisService.tryAcquire(botAuthorId, post.getAuthorId());
            if (!cooldownAcquired) {
                botCapRedisService.release(postId);
                throw new CooldownViolationException(
                    "Bot " + botAuthorId + " is in cooldown for human " + post.getAuthorId());
            }
        }
    }

    private Comment resolveParent(CreateCommentRequest request, UUID postId) {
        if (request.getParentCommentId() == null) {
            return null;
        }
        Comment parent = commentRepository.findById(request.getParentCommentId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Parent comment not found: " + request.getParentCommentId()));
        if (!parent.getPost().getId().equals(postId)) {
            throw new ResourceNotFoundException(
                "Parent comment does not belong to post: " + postId);
        }
        return parent;
    }

    private int computeDepth(Comment parent) {
        if (parent == null) {
            return 0;
        }
        return parent.getDepthLevel() + 1;
    }

    private Comment persistComment(Post post, Comment parentComment,
                                    CreateCommentRequest request, int depthLevel) {
        Comment comment = Comment.builder()
            .post(post)
            .parentComment(parentComment)
            .authorId(request.getAuthorId())
            .authorType(request.getAuthorType())
            .content(request.getContent())
            .depthLevel(depthLevel)
            .build();
        return commentRepository.save(comment);
    }

    private void updateVirality(UUID postId, AuthorType authorType) {
        if (authorType == AuthorType.BOT) {
            viralityRedisService.addBotReply(postId);
        } else {
            viralityRedisService.addHumanComment(postId);
        }
    }
}
