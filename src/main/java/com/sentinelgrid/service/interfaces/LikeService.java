package com.sentinelgrid.service.interfaces;

import java.util.UUID;

import com.sentinelgrid.dto.request.LikePostRequest;
import com.sentinelgrid.dto.response.LikeResponse;

public interface LikeService {
    LikeResponse likePost(UUID postId, LikePostRequest request);
}
