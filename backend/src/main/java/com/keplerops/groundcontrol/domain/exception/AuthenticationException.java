package com.keplerops.groundcontrol.domain.exception;

public class AuthenticationException extends GroundControlException {

    public AuthenticationException(String message) {
        super(message, "authentication_error");
    }
}
