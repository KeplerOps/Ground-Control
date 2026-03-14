package com.keplerops.groundcontrol.domain.exception;

public class NotFoundException extends GroundControlException {

    public NotFoundException(String message) {
        super(message, "not_found");
    }
}
