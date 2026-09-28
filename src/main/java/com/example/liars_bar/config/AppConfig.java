package com.example.liars_bar.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Random;

@Configuration
public class AppConfig {

    /**
     * SQLite: bitta ulanish (bir vaqtda bitta yozuvchi), WAL rejimi va busy_timeout.
     */
    @Bean(destroyMethod = "close")
    public DataSource dataSource(@Value("${app.db.path}") String dbPath) throws IOException {
        Path path = Path.of(dbPath).toAbsolutePath();
        Files.createDirectories(path.getParent());

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + path);
        config.setMaximumPoolSize(1);
        config.setPoolName("sqlite");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "5000");
        config.addDataSourceProperty("foreign_keys", "true");
        return new HikariDataSource(config);
    }

    /**
     * Kartalarni aralashtirish va o'q tartibi oldindan aytib bo'lmaydigan bo'lishi uchun.
     */
    @Bean
    public Random gameRandom() {
        return new SecureRandom();
    }
}
