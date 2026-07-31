package br.com.food.demo.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.security.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
