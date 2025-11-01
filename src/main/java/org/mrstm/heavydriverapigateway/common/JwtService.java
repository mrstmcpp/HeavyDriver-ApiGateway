package org.mrstm.heavydriverapigateway.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }


    public Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return null; // token invalid or expired
        }
    }

    public <T> T extractFromToken(String token, Function<Claims, T> claimsExtractor) {
        final Claims claims = extractAllClaims(token);
        if (claims == null) return null;
        return claimsExtractor.apply(claims);
    }


    public String extractEmailFromToken(String token) {
        return extractFromToken(token, Claims::getSubject);
    }

    public String extractRoleFromToken(String token) {
        return extractFromToken(token, claims -> (String) claims.get("role"));
    }

    public Long extractUserIdFromToken(String token) {
        Object userIdObj = extractFromToken(token, claims -> claims.get("userId"));
        if (userIdObj == null) return null;
        return Long.valueOf(userIdObj.toString());
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token); // will throw if invalid or expired
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
