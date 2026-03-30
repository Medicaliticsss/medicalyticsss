package com.medicalyticsss.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway forceFlyway(DataSource dataSource) {
        System.out.println("WYMUSZAM URUCHOMIENIE FLYWAYA");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        System.out.println("FLYWAY ZAKOŃCZYŁ PRACĘ");

        return flyway;
    }
}