package com.khaata.exception;

/**
 * Thrown when a request is syntactically valid but breaks a business rule
 * (e.g. repayment exceeds outstanding, duplicate phone). Maps to 409/400.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
