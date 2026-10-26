package com.khaata.dto;

import java.time.Instant;
import java.util.Map;

/** Uniform error body returned by the global exception handler. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {}
