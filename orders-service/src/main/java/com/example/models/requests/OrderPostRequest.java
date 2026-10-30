package com.example.models.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record OrderPostRequest(@NotEmpty List<@Valid ItemRequest> items) {
}