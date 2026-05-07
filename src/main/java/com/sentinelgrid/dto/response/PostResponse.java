package com.sentinelgrid.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.sentinelgrid.enums.AuthorType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostResponse {
    private final UUID id;
    private final UUID authorId;
    private final AuthorType authorType;
    private final String content;
    private final Instant createdAt;
}
