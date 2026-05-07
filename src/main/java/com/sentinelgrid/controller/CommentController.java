package com.sentinelgrid.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sentinelgrid.dto.request.CreateCommentRequest;
import com.sentinelgrid.dto.response.ApiResponse;
import com.sentinelgrid.dto.response.CommentResponse;
import com.sentinelgrid.service.interfaces.CommentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/posts")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.createComment(postId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Comment created successfully", response));
    }
}
