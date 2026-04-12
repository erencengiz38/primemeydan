package com.meydan.meydan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
@EnableScheduling // Zamanlanmış görevler (Doping kontrolü vb.) için eklendi
public class MeydanApplication {

    private static final Logger logger = LoggerFactory.getLogger(MeydanApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MeydanApplication.class, args);
    }

    @Bean
    public CommandLineRunner checkDatabaseConnection(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                logger.info("==================================================================================");
                logger.info("🎉 BAŞARILI: Veritabanına başarıyla bağlanıldı ve tablolar kontrol edildi/oluşturuldu!");
                logger.info("🔗 Bağlanılan Veritabanı URL: {}", connection.getMetaData().getURL());
                logger.info("==================================================================================");
            } catch (Exception e) {
                logger.error("==================================================================================");
                logger.error("❌ HATA: Veritabanına bağlanılamadı!");
                logger.error("❌ Hata detayı: {}", e.getMessage());
                logger.error("==================================================================================");
            }
        };
    }
}
