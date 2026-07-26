// backend/src/main/java/com/vantage/product/ui/dto/BulkUploadResponse.java
package com.vantage.product.ui.dto;

import java.util.List;

public record BulkUploadResponse(
    int totalRecords,
    int successCount,
    int failureCount,
    List<String> errors
) {
}
