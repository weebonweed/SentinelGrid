package com.sentinelgrid.service.interfaces;

import java.util.UUID;

import com.sentinelgrid.dto.request.CreateCommentRequest;
import com.sentinelgrid.dto.response.CommentResponse;

public interface CommentService {
    CommentResponse createComment(UUID postId, CreateCommentRequest request);
}
