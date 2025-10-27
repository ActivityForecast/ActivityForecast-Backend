package com.activityforecastbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] headers;
        if (allowedHeaders.equals("*")) {
            headers = new String[]{"*"};
        } else {
            headers = allowedHeaders.split(",");
        }
        
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(headers)
                .allowCredentials(allowCredentials)
                .maxAge(3600);
    }
}