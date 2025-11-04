package com.texttosql.apigateway.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends GatewayException {
    private final String clientIp;
    private final int retryAfter;

    public RateLimitExceededException(String clientIp, int retryAfter) {
        super(String.format("Rate limit exceeded for IP: %s. Retry after %d seconds",
                clientIp, retryAfter));
        this.clientIp = clientIp;
        this.retryAfter = retryAfter;
    }
}
