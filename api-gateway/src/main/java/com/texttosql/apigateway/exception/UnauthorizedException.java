package com.texttosql.apigateway.exception;

public class UnauthorizedException extends GatewayException {
    public UnauthorizedException(String message) {
        super(message);
    }
}