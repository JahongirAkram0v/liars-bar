package com.example.liars_bar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Random;

@Configuration
public class AppConfig {

    /**
     * Kartalarni aralashtirish va o'q tartibi oldindan aytib bo'lmaydigan bo'lishi uchun.
     */
    @Bean
    public Random gameRandom() {
        return new SecureRandom();
    }
}
