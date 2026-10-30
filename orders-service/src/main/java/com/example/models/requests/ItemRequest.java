package com.example.models.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ItemRequest(@NotBlank String productId, @Min(1) int quantity) {
}