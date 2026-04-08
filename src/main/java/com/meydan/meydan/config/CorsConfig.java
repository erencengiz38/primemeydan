package com.meydan.meydan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Uygulamadaki tüm endpointlere izin ver
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:5500","http://localhost:8080" ) // Hangi portlardan istek geleceğini buraya yaz (kendi frontend portunu ekle)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // İzin verilen HTTP metotları
                        .allowedHeaders("*") // Tüm headerlara (Özellikle Authorization header'ına) izin ver
                        .allowCredentials(true); // Token, cookie gibi kimlik doğrulama verilerine izin ver
            }
        };
    }
}