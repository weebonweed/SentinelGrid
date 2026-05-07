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
import com.sentinelgrid.dto.request.LikePostRequest;
import com.sentinelgrid.dto.response.LikeResponse;
import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.security.PayloadSizeFilter;
import com.sentinelgrid.service.interfaces.LikeService;

@WebMvcTest(LikeController.class)
@Import({SecurityConfig.class, PayloadSizeFilter.class})
class LikeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private LikeService likeService;
    @MockBean private com.sentinelgrid.config.SentinelGridProperties sentinelGridProperties;

    @Test
    void likePost_validRequest_returns201() throws Exception {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        LikePostRequest request = new LikePostRequest(authorId, AuthorType.HUMAN);

        LikeResponse response = LikeResponse.builder()
            .likeId(UUID.randomUUID())
            .postId(postId)
            .authorId(authorId)
            .authorType(AuthorType.HUMAN)
            .build();

        when(likeService.likePost(eq(postId), any(LikePostRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/posts/{postId}/like", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Post liked successfully"))
            .andExpect(jsonPath("$.data.authorType").value("HUMAN"));
    }

    @Test
    void likePost_missingAuthorId_returns400() throws Exception {
        UUID postId = UUID.randomUUID();
        LikePostRequest request = new LikePostRequest(null, AuthorType.HUMAN);

        mockMvc.perform(post("/api/posts/{postId}/like", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.authorId").exists());
    }

    @Test
    void likePost_missingAuthorType_returns400() throws Exception {
        UUID postId = UUID.randomUUID();
        LikePostRequest request = new LikePostRequest(UUID.randomUUID(), null);

        mockMvc.perform(post("/api/posts/{postId}/like", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.authorType").exists());
    }
}
