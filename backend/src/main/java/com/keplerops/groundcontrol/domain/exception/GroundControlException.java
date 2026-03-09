package com.keplerops.groundcontrol.domain.exception;

/**
 * Base exception for all Ground Control domain errors.
 */
public class GroundControlException extends RuntimeException {

    private final String errorCode;

    public GroundControlException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public GroundControlException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
