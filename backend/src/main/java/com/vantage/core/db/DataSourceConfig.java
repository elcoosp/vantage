package com.vantage.core.db;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.primary")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return primaryDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    public DataSource replicaDataSource() {
        return replicaDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public DataSource routingDataSource(@Qualifier("primaryDataSource") DataSource primary,
                                        @Qualifier("replicaDataSource") DataSource replica) {
        ReplicaRoutingDataSource routing = new ReplicaRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DatabaseType.PRIMARY, primary);
        targetDataSources.put(DatabaseType.REPLICA, replica);
        routing.setTargetDataSources(targetDataSources);
        routing.setDefaultTargetDataSource(primary);
        routing.setLenientFallback(false);
        return routing;
    }
}
