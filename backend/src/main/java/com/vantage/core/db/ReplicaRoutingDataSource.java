package com.vantage.core.db;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class ReplicaRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        // Always return PRIMARY for now – test will fail because REPLICA not used
        return DatabaseType.PRIMARY;
    }
}
