package com.khaata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config for the Resend HTTP email API (https://resend.com).
 * Used instead of SMTP because cloud hosts (e.g. Render free tier) block SMTP ports,
 * whereas Resend sends over HTTPS (port 443), which is always allowed.
 */
@ConfigurationProperties(prefix = "resend")
public record ResendProperties(
        String apiKey,
        String from
) {}
