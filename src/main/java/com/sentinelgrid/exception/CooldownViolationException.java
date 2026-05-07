package com.sentinelgrid.exception;

public class CooldownViolationException extends RuntimeException {
    public CooldownViolationException(String message) {
        super(message);
    }
}
