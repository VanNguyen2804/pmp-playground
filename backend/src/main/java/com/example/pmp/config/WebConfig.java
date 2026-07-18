package com.example.pmp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;

    public WebConfig(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String origins,
            @Value("${app.cors.allowed-hosts:}") String hosts
    ) {
        Set<String> values = new LinkedHashSet<>();
        addCsv(values, origins, false);
        addCsv(values, hosts, true);
        this.allowedOrigins = values.toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    private static void addCsv(Set<String> values, String csv, boolean hostOnly) {
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> hostOnly && !value.contains("://") ? "https://" + value : value)
                .map(value -> value.endsWith("/") ? value.substring(0, value.length() - 1) : value)
                .forEach(values::add);
    }
}
