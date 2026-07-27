package com.vantage.product.ui;

import com.vantage.product.app.ProductService;
import com.vantage.api.api.ApiApi;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController implements ApiApi {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable UUID id) {
        return productService.getProductById(id);
    }

    @Override
    public ResponseEntity<com.vantage.api.model.ProductResponse> apiV1ProductsPost(com.vantage.api.model.ProductRequest productRequest) {
        com.vantage.product.ui.dto.ProductRequest internalRequest =
            new com.vantage.product.ui.dto.ProductRequest(
                productRequest.getName(),
                productRequest.getDescription(),
                java.math.BigDecimal.valueOf(productRequest.getPrice())
            );
        com.vantage.product.ui.dto.ProductResponse internalResponse = productService.createProduct(internalRequest);
        com.vantage.api.model.ProductResponse response = new com.vantage.api.model.ProductResponse()
            .id(internalResponse.id())
            .name(internalResponse.name())
            .price(internalResponse.price());
        // sku not in internalResponse, so we skip it or set null
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}