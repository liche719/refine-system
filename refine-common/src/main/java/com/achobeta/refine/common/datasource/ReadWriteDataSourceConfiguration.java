package com.achobeta.refine.common.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "refine.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReadWriteDataSourceConfiguration {

    @Bean
    @Primary
    DataSource dataSource(
            @Value("${refine.datasource.primary-url}") String primaryUrl,
            @Value("${refine.datasource.replica-url}") String replicaUrl,
            @Value("${refine.datasource.username}") String username,
            @Value("${refine.datasource.password}") String password,
            @Value("${refine.datasource.maximum-pool-size:10}") int maximumPoolSize) {
        HikariDataSource primary = create("primary", primaryUrl, username, password, maximumPoolSize, true);
        HikariDataSource replica = create("replica", replicaUrl, username, password, maximumPoolSize, false);
        FallbackDataSource fallbackReplica = new FallbackDataSource(replica, primary);

        AbstractRoutingDataSource routing = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return ReadWriteContext.isReplica() ? "replica" : "primary";
            }
        };
        routing.setDefaultTargetDataSource(primary);
        routing.setTargetDataSources(Map.of("primary", primary, "replica", fallbackReplica));
        routing.afterPropertiesSet();
        return routing;
    }

    private HikariDataSource create(String name, String url, String username, String password,
                                    int maximumPoolSize, boolean failFast) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("refine-" + name);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5_000);
        config.setInitializationFailTimeout(failFast ? 1 : -1);
        return new HikariDataSource(config);
    }
}
