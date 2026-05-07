package com.sentinelgrid.dto.response;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String path;
    private final String correlationId;
    private final Map<String, String> fieldErrors;
}
