// backend/src/main/java/com/vantage/product/app/BulkUploadService.java
package com.vantage.product.app;

import com.vantage.core.tenant.TenantContext;
import com.vantage.product.domain.Product;
import com.vantage.product.domain.ProductRepository;
import com.vantage.product.ui.dto.BulkUploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class BulkUploadService {

    private final ProductRepository productRepository;

    public BulkUploadService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public BulkUploadResponse processBulkUpload(MultipartFile file) {
        // HUMAN REVIEW REQUIRED: Add 'com.opencsv:opencsv:5.9' to build.gradle.kts for robust CSV parsing.
        // Current implementation uses simple split which may fail on values containing commas.

        List<String> errors = new ArrayList<>();
        int totalRecords = 0;
        int successCount = 0;
        int failureCount = 0;

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            String line = reader.readLine();
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                totalRecords++;
                final int recordNumber = totalRecords;
                final String currentLine = line;

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    TenantContext.setTenantId(tenantId);
                    try {
                        String[] parts = currentLine.split(",");
                        if (parts.length < 2) {
                            throw new IllegalArgumentException("Invalid CSV format: missing columns");
                        }
                        String name = parts[0].trim();
                        BigDecimal price = new BigDecimal(parts[1].trim());
                        String description = parts.length > 2 ? parts[2].trim() : "";

                        if (name.isEmpty()) {
                            throw new IllegalArgumentException("Name cannot be empty");
                        }
                        if (price.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException("Price must be greater than 0");
                        }

                        Product product = new Product();
                        product.setName(name);
                        product.setPrice(price);
                        product.setDescription(description);

                        productRepository.save(product);
                    } catch (Exception e) {
                        throw new RuntimeException("Row " + recordNumber + ": " + e.getMessage(), e);
                    } finally {
                        TenantContext.clear();
                    }
                }, executor);

                futures.add(future);
            }

            for (CompletableFuture<Void> future : futures) {
                try {
                    future.join();
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    Throwable cause = e.getCause();
                    errors.add(cause != null ? cause.getMessage() : e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to process CSV file: " + e.getMessage(), e);
        }

        return new BulkUploadResponse(totalRecords, successCount, failureCount, errors);
    }
}
