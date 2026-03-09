package com.keplerops.groundcontrol.domain.exception;

public class ConflictException extends GroundControlException {

    public ConflictException(String message) {
        super(message, "conflict");
    }
}
