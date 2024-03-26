package com.example.demo.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class BusinessBotConfig {
    @Value("${bbot.username}")
    private String botName;
    @Value("${bbot.token}")
    private String token;
}
