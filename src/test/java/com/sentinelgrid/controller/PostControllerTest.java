package com.sentinelgrid.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelgrid.config.SecurityConfig;
import com.sentinelgrid.dto.request.CreatePostRequest;
import com.sentinelgrid.dto.response.PostResponse;
import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.security.PayloadSizeFilter;
import com.sentinelgrid.service.interfaces.PostService;

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, PayloadSizeFilter.class})
class PostControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PostService postService;
    @MockBean private com.sentinelgrid.config.SentinelGridProperties sentinelGridProperties;

    @Test
    void createPost_validRequest_returns201() throws Exception {
        UUID authorId = UUID.randomUUID();
        CreatePostRequest request = new CreatePostRequest(authorId, AuthorType.HUMAN, "Hello world");

        PostResponse response = PostResponse.builder()
            .id(UUID.randomUUID())
            .authorId(authorId)
            .authorType(AuthorType.HUMAN)
            .content("Hello world")
            .build();

        when(postService.createPost(any(CreatePostRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Post created successfully"))
            .andExpect(jsonPath("$.data.authorType").value("HUMAN"));
    }

    @Test
    void createPost_missingContent_returns400() throws Exception {
        CreatePostRequest request = new CreatePostRequest(UUID.randomUUID(), AuthorType.HUMAN, "");

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.content").exists());
    }

    @Test
    void createPost_missingAuthorId_returns400() throws Exception {
        CreatePostRequest request = new CreatePostRequest(null, AuthorType.HUMAN, "Some content");

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.authorId").exists());
    }

    @Test
    void createPost_missingAuthorType_returns400() throws Exception {
        CreatePostRequest request = new CreatePostRequest(UUID.randomUUID(), null, "Some content");

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.authorType").exists());
    }
}
