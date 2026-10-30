package com.example.models.requests;

import io.smallrye.common.constraint.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProductPostRequest(@NotBlank String sku, @NotBlank String name,
        @NotNull @DecimalMin("0.00") double price, @Min(0) int stock) {
}
