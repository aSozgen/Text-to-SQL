package com.texttosql.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SchemaImportException extends RuntimeException {

    public SchemaImportException(String message) {
        super(message);
    }

    public SchemaImportException(String message, Throwable cause) {
        super(message, cause);
    }
}