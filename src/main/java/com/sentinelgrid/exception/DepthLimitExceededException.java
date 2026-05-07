package com.sentinelgrid.exception;

public class DepthLimitExceededException extends RuntimeException {
    public DepthLimitExceededException(String message) {
        super(message);
    }
}
