package com.texttosql.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired");
            throw e;
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed");
            throw e;
        } catch (SignatureException e) {
            log.error("JWT signature validation failed");
            throw e;
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            throw e;
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);

            if (isTokenExpired(token)) {
                log.warn("Token is expired");
                return false;
            }

            String userId = extractUserId(token);
            String username = extractUsername(token);

            if (userId == null || userId.isEmpty()) {
                log.error("Token missing userId claim");
                return false;
            }

            if (username == null || username.isEmpty()) {
                log.error("Token missing username claim");
                return false;
            }

            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Token validation failed: expired");
            return false;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }


    public String getValidationError(String token) {
        try {
            validateToken(token);
            return null;
        } catch (ExpiredJwtException e) {
            return "Token has expired";
        } catch (MalformedJwtException e) {
            return "Token is malformed";
        } catch (SignatureException e) {
            return "Token signature is invalid";
        } catch (Exception e) {
            return "Token validation failed: " + e.getMessage();
        }
    }
}