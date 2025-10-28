package com.activityforecastbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;
    
    private final Environment environment;
    
    public SwaggerConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(getServerList())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(
                        new Components()
                                .addSecuritySchemes("bearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .in(SecurityScheme.In.HEADER)
                                                .name("Authorization")
                                )
                );
    }
    
    private List<Server> getServerList() {
        List<Server> servers = new ArrayList<>();
        
        // Primary: Relative URL server (automatically uses current browser protocol/host)
        servers.add(new Server()
                .url("/")
                .description("Current Host (Recommended)"));
        
        // Local development server
        servers.add(new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server"));
        
        // Check if running in production
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = false;
        for (String profile : activeProfiles) {
            if ("prod".equals(profile)) {
                isProduction = true;
                break;
            }
        }
        
        if (isProduction) {
            // Production server (HTTP - as currently deployed)
            servers.add(new Server()
                    .url("http://144.24.73.5:8080")
                    .description("Production Server"));
        } else {
            // Docker development server
            servers.add(new Server()
                    .url("http://localhost")
                    .description("Docker Development Server"));
        }
        
        return servers;
    }

    private Info apiInfo() {
        return new Info()
                .title("ActivityForecast Backend API")
                .description("AI 기반 개인화 야외활동 추천 서비스 '활동예보'의 REST API 문서")
                .version("1.0.0")
                .contact(new Contact()
                        .name("ActivityForecast Development Team")
                        .email("dev@activityforecast.com")
                        .url("https://github.com/your-username/ActivityForecast-Backend")
                );
    }
}