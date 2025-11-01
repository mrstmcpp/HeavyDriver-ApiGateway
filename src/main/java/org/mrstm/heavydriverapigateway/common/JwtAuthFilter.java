package org.mrstm.heavydriverapigateway.common;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

//@Component
public class JwtAuthFilter implements WebFilter {

    private final JwtService jwtService;

    @Autowired
    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = null;

        // 1. Try to get token from cookie
        if (exchange.getRequest().getCookies().containsKey("JwtToken")) {
            token = exchange.getRequest().getCookies().getFirst("JwtToken").getValue();
        }
        // 2. If not in cookie, try to get from Authorization header
        else if (exchange.getRequest().getHeaders().containsKey("Authorization")) {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // 3. If token is missing or invalid, block the request
        if (token == null || !jwtService.isTokenValid(token)) {
            System.out.println("Token is invalid or missing.");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        System.out.println("Token is valid. Proceeding with claims extraction.");

        // 4. Extract claims from the valid token
        Claims claims = jwtService.extractAllClaims(token);
        String email = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class); // e.g., "PASSENGER"

        System.out.println("User Details: " + email + " " + userId + " " + role);

        // 5. Create the Authentication object for Spring Security
        // Note: Roles in Spring Security usually start with "ROLE_"
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, // or userId, whatever you prefer as the principal
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );

        // 6. Mutate the request to add headers for downstream services
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Email", email)
                .header("X-User-Role", role)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        // 7. ***** THIS IS THE FIX *****
        //    Pass the mutated exchange to the chain AND write the Authentication
        //    object into the reactive SecurityContext.
        return chain.filter(mutatedExchange)
                .contextWrite(context -> {
                    // Create a new SecurityContext with our Authentication
                    SecurityContext securityContext = new SecurityContextImpl(authentication);
                    // Add this SecurityContext to the reactive Context
                    return context.put(SecurityContext.class, Mono.just(securityContext));
                });
    }
}
