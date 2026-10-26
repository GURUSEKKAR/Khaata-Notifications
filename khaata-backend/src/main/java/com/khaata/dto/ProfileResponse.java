package com.khaata.dto;

/** The owner's profile as shown on the profile page. */
public record ProfileResponse(
        String shopName,
        String ownerName,
        String phone,
        String email,
        String upiId
) {}
