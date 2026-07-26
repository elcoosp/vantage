// backend/src/main/java/com/vantage/product/ui/BulkUploadController.java
package com.vantage.product.ui;

import com.vantage.product.app.BulkUploadService;
import com.vantage.product.ui.dto.BulkUploadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
public class BulkUploadController {

    private final BulkUploadService bulkUploadService;

    public BulkUploadController(BulkUploadService bulkUploadService) {
        this.bulkUploadService = bulkUploadService;
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<BulkUploadResponse> bulkUpload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        BulkUploadResponse response = bulkUploadService.processBulkUpload(file);
        return ResponseEntity.ok(response);
    }
}
