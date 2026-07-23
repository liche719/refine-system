package com.achobeta.refine.ai.rag;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(PgVectorProperties.class)
@ConditionalOnProperty(prefix = "refine.pgvector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PgVectorConfiguration {
    @Bean(name = "pgVectorDataSource", destroyMethod = "close")
    DataSource pgVectorDataSource(PgVectorProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("refine-pgvector");
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5_000);
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    @Bean("pgVectorJdbcTemplate")
    JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
