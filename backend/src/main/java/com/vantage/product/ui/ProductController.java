package com.vantage.product.ui;

import com.vantage.product.app.ProductService;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Product CRUD. The endpoints intentionally use the internal DTOs (richer than
 * the generated contract models); the generated {@code ApiApi} interface is
 * NOT implemented here because Spring MVC would register its default method
 * mappings in addition to these, causing ambiguous-mapping failures at boot.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @GetMapping
    public List<ProductResponse> listProducts() {
        return productService.listProducts();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable UUID id) {
        return productService.getProductById(id);
    }

}