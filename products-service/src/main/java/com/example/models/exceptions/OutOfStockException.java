package com.example.models.exceptions;

public class OutOfStockException extends RuntimeException {
    public final String productId;

    public OutOfStockException(String productId) {
        super("Insufficient stock for product " + productId);
        this.productId = productId;
    }
}