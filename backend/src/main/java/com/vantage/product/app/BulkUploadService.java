// backend/src/main/java/com/vantage/product/app/BulkUploadService.java
package com.vantage.product.app;

// HUMAN REVIEW REQUIRED: Add 'com.opencsv:opencsv:5.9' to build.gradle.kts for robust CSV parsing.
// Current implementation uses simple split which may fail on values containing commas.

import com.vantage.core.tenant.TenantContext;
import com.vantage.product.domain.Product;
import com.vantage.product.domain.ProductRepository;
import com.vantage.product.ui.dto.BulkUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final Logger log = LoggerFactory.getLogger(BulkUploadService.class);
    private final ProductRepository productRepository;
    private static final int BATCH_SIZE = 50;

    public BulkUploadService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public BulkUploadResponse processBulkUpload(MultipartFile file) {
        log.info("Starting bulk upload processing");
        List<String> errors = new ArrayList<>();
        List<Product> validProducts = new ArrayList<>();
        int totalRecords = 0;
        int failureCount = 0;

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line = reader.readLine(); // Skip header
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                totalRecords++;

                String[] parts = line.split(",", -1);
                if (parts.length < 2) {
                    errors.add("Row " + lineNumber + ": Invalid CSV format, missing columns");
                    failureCount++;
                    continue;
                }

                String name = parts[0].trim();
                String priceStr = parts[1].trim();
                String description = parts.length > 2 ? parts[2].trim() : "";

                if (name.isEmpty()) {
                    errors.add("Row " + lineNumber + ": Name cannot be empty");
                    failureCount++;
                    continue;
                }

                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr);
                } catch (NumberFormatException e) {
                    errors.add("Row " + lineNumber + ": Invalid price format '" + priceStr + "'");
                    failureCount++;
                    continue;
                }

                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Row " + lineNumber + ": Price must be greater than 0");
                    failureCount++;
                    continue;
                }

                Product product = new Product();
                product.setName(name);
                product.setPrice(price);
                product.setDescription(description);
                validProducts.add(product);
            }
        } catch (Exception e) {
            log.error("Failed to read CSV file", e);
            throw new IllegalArgumentException("Failed to process CSV file: " + e.getMessage(), e);
        }

        int successCount = 0;
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < validProducts.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, validProducts.size());
                List<Product> batch = new ArrayList<>(validProducts.subList(i, end));

                CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                    TenantContext.setTenantId(tenantId);
                    try {
                        return saveBatch(batch);
                    } finally {
                        TenantContext.clear();
                    }
                }, executor);
                futures.add(future);
            }

            for (CompletableFuture<Integer> future : futures) {
                try {
                    successCount += future.join();
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    errors.add("Batch save failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
                }
            }
        }

        log.info("Bulk upload completed. Total: {}, Success: {}, Failures: {}", totalRecords, successCount, failureCount);
        return new BulkUploadResponse(totalRecords, successCount, failureCount, errors);
    }

    @Transactional
    public int saveBatch(List<Product> batch) {
        productRepository.saveAll(batch);
        return batch.size();
    }
}
