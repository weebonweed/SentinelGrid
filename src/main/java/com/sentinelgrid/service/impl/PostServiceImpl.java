package com.sentinelgrid.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelgrid.dto.request.CreatePostRequest;
import com.sentinelgrid.dto.response.PostResponse;
import com.sentinelgrid.entity.Post;
import com.sentinelgrid.mapper.EntityMapper;
import com.sentinelgrid.repository.PostRepository;
import com.sentinelgrid.service.interfaces.PostService;
import com.sentinelgrid.util.AuthorResolver;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final AuthorResolver authorResolver;
    private final EntityMapper entityMapper;

    public PostServiceImpl(PostRepository postRepository,
                           AuthorResolver authorResolver,
                           EntityMapper entityMapper) {
        this.postRepository = postRepository;
        this.authorResolver = authorResolver;
        this.entityMapper = entityMapper;
    }

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        authorResolver.assertExists(request.getAuthorId(), request.getAuthorType());

        Post post = Post.builder()
            .authorId(request.getAuthorId())
            .authorType(request.getAuthorType())
            .content(request.getContent())
            .build();

        Post saved = postRepository.save(post);
        log.info("Post created: id={} authorId={} authorType={}",
            saved.getId(), saved.getAuthorId(), saved.getAuthorType());
        return entityMapper.toPostResponse(saved);
    }
}
