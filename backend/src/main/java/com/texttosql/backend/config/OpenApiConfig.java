package com.texttosql.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Natural Language to SQL Conversion Using LLMs")
                        .version("1.0.0")
                        .description("This API is designed to translate natural language queries into structured SQL commands using AI-powered Large Language Models. " +
                                "It aims to simplify data access for non-technical users by bridging the gap between natural language and database queries.")
                        .contact(new Contact()
                                .name("Ramazan Bozkurt & Abdulkadir Sözgen")
                                .email("ramazan.bozkurt.dev@gmail.com"))
                );
    }
}