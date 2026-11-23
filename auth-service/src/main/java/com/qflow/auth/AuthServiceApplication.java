package com.qflow.auth;

import com.qflow.common.security.JwtService;
import com.qflow.common.web.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    public JwtService jwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long minutes) {
        return new JwtService(secret, minutes);
    }
}