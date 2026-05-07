package com.sentinelgrid.dto.request;

import java.util.UUID;

import com.sentinelgrid.enums.AuthorType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikePostRequest {

    @NotNull(message = "authorId is required")
    private UUID authorId;

    @NotNull(message = "authorType is required")
    private AuthorType authorType;
}
