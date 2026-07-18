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
    private final String[] allowedOriginPatterns;

    public WebConfig(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String origins,
            @Value("${app.cors.allowed-hosts:}") String hosts,
            @Value("${app.cors.allowed-origin-patterns:}") String originPatterns
    ) {
        Set<String> originValues = new LinkedHashSet<>();
        addCsv(originValues, origins, false);
        addCsv(originValues, hosts, true);
        this.allowedOrigins = originValues.toArray(String[]::new);

        Set<String> patternValues = new LinkedHashSet<>();
        addCsv(patternValues, originPatterns, false);
        this.allowedOriginPatterns = patternValues.toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        if (allowedOrigins.length > 0) {
            registration.allowedOrigins(allowedOrigins);
        }
        if (allowedOriginPatterns.length > 0) {
            registration.allowedOriginPatterns(allowedOriginPatterns);
        }
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
