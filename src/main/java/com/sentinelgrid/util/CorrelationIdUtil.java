package com.sentinelgrid.util;

import java.util.UUID;

public final class CorrelationIdUtil {

    private CorrelationIdUtil() {}

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
