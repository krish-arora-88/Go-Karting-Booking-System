package com.gokarting.domain.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String username) {
        super("Username '%s' is already taken".formatted(username));
    }
}
