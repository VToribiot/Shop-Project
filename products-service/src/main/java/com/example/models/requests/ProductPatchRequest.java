package com.example.models.requests;

import jakarta.validation.constraints.DecimalMin;

public record ProductPatchRequest(String name, @DecimalMin("0.00") Double price) {
}
