package com.vantage;

import com.vantage.core.tenant.TenantJpaRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(repositoryBaseClass = TenantJpaRepositoryImpl.class)
public class VantageApplication {
    public static void main(String[] args) {
        SpringApplication.run(VantageApplication.class, args);
    }
}
