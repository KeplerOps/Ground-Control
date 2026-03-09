package com.keplerops.groundcontrol.domain.exception;

public class AuthorizationException extends GroundControlException {

    public AuthorizationException(String message) {
        super(message, "authorization_error");
    }
}
