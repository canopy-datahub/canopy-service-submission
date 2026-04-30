package org.canopyplatform.canopy.submissionservice.config;

import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "database")
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
public class DatabaseConfig {

    private String dbUser, dbPassword, host, port, dbName, dbDriverClassName;

    @Primary
    @Bean
    public DataSource getDataSource() {
        return DataSourceBuilder.create()
                .driverClassName(dbDriverClassName)
                .username(dbUser)
                .password(dbPassword)
                .url("jdbc:postgresql://" + host + ":" + port + "/" + dbName)
                .build();
    }
}
