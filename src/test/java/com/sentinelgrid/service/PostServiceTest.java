package com.sentinelgrid.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sentinelgrid.dto.request.CreatePostRequest;
import com.sentinelgrid.dto.response.PostResponse;
import com.sentinelgrid.entity.Post;
import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.exception.ResourceNotFoundException;
import com.sentinelgrid.mapper.EntityMapper;
import com.sentinelgrid.repository.PostRepository;
import com.sentinelgrid.service.impl.PostServiceImpl;
import com.sentinelgrid.util.AuthorResolver;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private AuthorResolver authorResolver;
    @Mock private EntityMapper entityMapper;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    void createPost_humanAuthor_succeeds() {
        UUID authorId = UUID.randomUUID();
        CreatePostRequest request = new CreatePostRequest(authorId, AuthorType.HUMAN, "Hello world");

        Post savedPost = Post.builder()
            .id(UUID.randomUUID())
            .authorId(authorId)
            .authorType(AuthorType.HUMAN)
            .content("Hello world")
            .build();

        PostResponse expectedResponse = PostResponse.builder()
            .id(savedPost.getId())
            .authorId(authorId)
            .authorType(AuthorType.HUMAN)
            .content("Hello world")
            .build();

        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(entityMapper.toPostResponse(savedPost)).thenReturn(expectedResponse);

        PostResponse result = postService.createPost(request);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        assertThat(result.getAuthorType()).isEqualTo(AuthorType.HUMAN);
    }

    @Test
    void createPost_authorNotFound_throwsResourceNotFoundException() {
        UUID authorId = UUID.randomUUID();
        CreatePostRequest request = new CreatePostRequest(authorId, AuthorType.HUMAN, "Hello");

        doThrow(new ResourceNotFoundException("HUMAN author not found: " + authorId))
            .when(authorResolver).assertExists(authorId, AuthorType.HUMAN);

        assertThatThrownBy(() -> postService.createPost(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(authorId.toString());
    }
}
