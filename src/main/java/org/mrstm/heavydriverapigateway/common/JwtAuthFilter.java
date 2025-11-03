package org.mrstm.heavydriverapigateway.common;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpMethod; // Make sure this is imported
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

public class JwtAuthFilter implements WebFilter {

    private final JwtService jwtService;

    private final List<String> publicPaths = List.of(
            "/api/v1/auth/"
    );


    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = request.getPath().toString();
        boolean isPublic = publicPaths.stream().anyMatch(publicPath -> path.startsWith(publicPath));

        if (isPublic) {
            return chain.filter(exchange);
        }

        String token = null;

        if (exchange.getRequest().getCookies().containsKey("JwtToken")) {
            token = exchange.getRequest().getCookies().getFirst("JwtToken").getValue();
        } else if (exchange.getRequest().getHeaders().containsKey("Authorization")) {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null || !jwtService.isTokenValid(token)) {
            System.out.println("Token is invalid or missing for protected route: " + path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }


        Claims claims = jwtService.extractAllClaims(token);
        String email = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Email", email)
                .header("X-User-Role", role)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        return chain.filter(mutatedExchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }
}