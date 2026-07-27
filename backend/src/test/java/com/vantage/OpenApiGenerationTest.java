package com.vantage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class OpenApiGenerationTest {

    @Test
    void should_load_generated_product_response_model() {
        assertDoesNotThrow(() -> {
            Class.forName("com.vantage.api.model.ProductResponse");
        }, "Generated ProductResponse class should be present");
    }

    @Test
    void should_load_generated_order_request_model() {
        assertDoesNotThrow(() -> {
            Class.forName("com.vantage.api.model.OrderRequest");
        }, "Generated OrderRequest class should be present");
    }

    @Test
    void should_load_generated_product_api_interface() {
        assertDoesNotThrow(() -> {
            Class.forName("com.vantage.api.api.ApiApi");
        }, "Generated ApiApi interface should be present");
    }
}
