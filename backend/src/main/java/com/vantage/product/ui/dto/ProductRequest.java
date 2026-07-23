package com.vantage.product.ui.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductRequest(
    @NotBlank String name,
    String description,
    @NotNull @Positive Double price
) {
}
