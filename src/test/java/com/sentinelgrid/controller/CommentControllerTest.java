package com.sentinelgrid.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sentinelgrid.dto.request.CreateCommentRequest;
import com.sentinelgrid.dto.response.CommentResponse;
import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.exception.RateLimitExceededException;
import com.sentinelgrid.security.PayloadSizeFilter;
import com.sentinelgrid.service.interfaces.CommentService;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, PayloadSizeFilter.class})
class CommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommentService commentService;
    @MockBean private com.sentinelgrid.config.SentinelGridProperties sentinelGridProperties;

    @Test
    void createComment_validRequest_returns201() throws Exception {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest(null, authorId, AuthorType.HUMAN, "Nice post");

        CommentResponse response = CommentResponse.builder()
            .id(UUID.randomUUID())
            .postId(postId)
            .authorId(authorId)
            .authorType(AuthorType.HUMAN)
            .content("Nice post")
            .depthLevel(0)
            .build();

        when(commentService.createComment(eq(postId), any(CreateCommentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.depthLevel").value(0))
            .andExpect(jsonPath("$.data.authorType").value("HUMAN"));
    }

    @Test
    void createComment_botCapExceeded_returns429() throws Exception {
        UUID postId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest(null, UUID.randomUUID(), AuthorType.BOT, "Bot reply");

        when(commentService.createComment(eq(postId), any(CreateCommentRequest.class)))
            .thenThrow(new RateLimitExceededException("Bot reply cap reached for postId: " + postId));

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void createComment_blankContent_returns400() throws Exception {
        UUID postId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest(null, UUID.randomUUID(), AuthorType.HUMAN, "");

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.content").exists());
    }

    @Test
    void createComment_missingAuthorId_returns400() throws Exception {
        UUID postId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest(null, null, AuthorType.HUMAN, "content");

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.authorId").exists());
    }
}
