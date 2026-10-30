package com.example.models;

public final class OrderStatus {
    public static final String PENDING = "PENDING"; // saved, waiting for stock reservation
    public static final String CONFIRMED = "CONFIRMED"; // stock reserved
    public static final String REJECTED = "REJECTED"; // stock not available
    public static final String CANCELLED = "CANCELLED"; // cancelled by the customer after confirmation

    private OrderStatus() {
    }
}
