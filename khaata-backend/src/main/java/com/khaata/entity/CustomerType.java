package com.khaata.entity;

/**
 * Classifies a customer, which decides the default credit due window.
 * REGULAR customers (trusted) get a longer window; NORMAL customers a short one.
 */
public enum CustomerType {
    REGULAR,
    NORMAL
}
