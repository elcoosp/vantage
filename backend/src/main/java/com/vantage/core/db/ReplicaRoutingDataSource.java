package com.vantage.core.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class ReplicaRoutingDataSource extends AbstractRoutingDataSource {
    private static final Logger log = LoggerFactory.getLogger(ReplicaRoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        DatabaseType key = DatabaseContextHolder.getDatabaseType();
        log.info("Routing datasource: {}", key);
        return key;
    }
}
