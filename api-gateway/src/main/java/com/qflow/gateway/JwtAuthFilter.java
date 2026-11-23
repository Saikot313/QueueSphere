package com.qflow.gateway;

import com.qflow.common.constants.HeaderNames;
import com.qflow.common.security.JwtPrincipal;
import com.qflow.common.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PREFIXES = List.of("/api/auth/login", "/api/auth/register");

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Strip any identity headers sent by the client (anti-spoofing)
        ServerHttpRequest.Builder builder = request.mutate().headers(h -> {
            h.remove(HeaderNames.X_USER_ID);
            h.remove(HeaderNames.X_TENANT_ID);
            h.remove(HeaderNames.X_ROLE);
            h.remove(HeaderNames.X_USERNAME);
        });

        if (PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        try {
            JwtPrincipal p = jwtService.parse(auth.substring(7));
            builder.header(HeaderNames.X_USER_ID, String.valueOf(p.userId()));
            builder.header(HeaderNames.X_TENANT_ID, String.valueOf(p.tenantId()));
            builder.header(HeaderNames.X_ROLE, p.role());
            builder.header(HeaderNames.X_USERNAME, p.username());
            return chain.filter(exchange.mutate().request(builder.build()).build());
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}