package com.example.bajaj_hiring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "student")
public class AppConfig {
    private String name;
    private String regNo;
    private String email;

    // getters & setters
}
