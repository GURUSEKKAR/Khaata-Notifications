package com.khaata.dto;

/** Returned on successful login: the JWT plus a little display info. */
public record LoginResponse(
        String token,
        String username,
        String displayName
) {}
