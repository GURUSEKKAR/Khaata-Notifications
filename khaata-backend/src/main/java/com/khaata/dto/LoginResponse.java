package com.khaata.dto;

/** Returned on successful login/signup: the JWT plus shop display info. */
public record LoginResponse(
        String token,
        String shopName,
        String ownerName
) {}
