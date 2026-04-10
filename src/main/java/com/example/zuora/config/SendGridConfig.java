package com.example.zuora.config;

import com.sendgrid.SendGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SendGridConfig {

    private static final Logger logger = LoggerFactory.getLogger(SendGridConfig.class);

    @Value("${sendgrid.api-key:}")
    private String sendGridApiKey;

    @Bean
    public SendGrid sendGrid() {
        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            logger.warn("SendGrid API key not configured. Email sending will be disabled.");
            // Return a SendGrid with placeholder key - emails will fail silently
            return new SendGrid("placeholder-key");
        }
        return new SendGrid(sendGridApiKey);
    }
}