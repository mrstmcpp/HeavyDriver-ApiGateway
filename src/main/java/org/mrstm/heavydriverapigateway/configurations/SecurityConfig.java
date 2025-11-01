package org.mrstm.heavydriverapigateway.configurations;

import org.mrstm.heavydriverapigateway.common.JwtAuthFilter;
import org.mrstm.heavydriverapigateway.common.JwtService; // <-- Import JwtService
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // Inject JwtService, which is needed to create the filter
    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // *** FIX 1: Create the bean here ***
    // This bean will be created once and available for injection
    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtService);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         JwtAuthFilter jwtAuthFilter) { // Spring injects the bean from our method above
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // *** FIX 2: Correctly permit auth paths ***
                        .pathMatchers("/api/v1/auth/**").permitAll() // Allow login/signup
                        .pathMatchers("/api/v1/booking/**").permitAll() // Allow login/signup
                        .pathMatchers("/api/v1/review/**").permitAll() // Allow login/signup
                        .anyExchange().authenticated() // Secure everything else
                )
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                // This line now works perfectly
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }
}
