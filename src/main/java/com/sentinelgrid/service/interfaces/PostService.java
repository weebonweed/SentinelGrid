package com.sentinelgrid.service.interfaces;

import com.sentinelgrid.dto.request.CreatePostRequest;
import com.sentinelgrid.dto.response.PostResponse;

public interface PostService {
    PostResponse createPost(CreatePostRequest request);
}
