package com.texttosql.backend.exception;

public class NotResourceOwnerException extends RuntimeException {
    public NotResourceOwnerException(String message) {
        super(message);
    }
}