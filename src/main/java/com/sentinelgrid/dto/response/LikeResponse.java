package com.sentinelgrid.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.sentinelgrid.enums.AuthorType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeResponse {
    private final UUID likeId;
    private final UUID postId;
    private final UUID authorId;
    private final AuthorType authorType;
    private final Instant createdAt;
}
