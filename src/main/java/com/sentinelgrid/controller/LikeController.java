package com.sentinelgrid.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sentinelgrid.dto.request.LikePostRequest;
import com.sentinelgrid.dto.response.ApiResponse;
import com.sentinelgrid.dto.response.LikeResponse;
import com.sentinelgrid.service.interfaces.LikeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/posts")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> likePost(
            @PathVariable UUID postId,
            @Valid @RequestBody LikePostRequest request) {
        LikeResponse response = likeService.likePost(postId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Post liked successfully", response));
    }
}
