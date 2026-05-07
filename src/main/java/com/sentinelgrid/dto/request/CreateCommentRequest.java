package com.sentinelgrid.dto.request;

import java.util.UUID;

import com.sentinelgrid.enums.AuthorType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    private UUID parentCommentId;

    @NotNull(message = "authorId is required")
    private UUID authorId;

    @NotNull(message = "authorType is required")
    private AuthorType authorType;

    @NotBlank(message = "content must not be blank")
    @Size(min = 1, max = 1000, message = "content must be between 1 and 1000 characters")
    private String content;
}
