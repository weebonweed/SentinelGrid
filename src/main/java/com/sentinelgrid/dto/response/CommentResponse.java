package com.sentinelgrid.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sentinelgrid.enums.AuthorType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {
    private final UUID id;
    private final UUID postId;
    private final UUID parentCommentId;
    private final UUID authorId;
    private final AuthorType authorType;
    private final String content;
    private final int depthLevel;
    private final Instant createdAt;
}
