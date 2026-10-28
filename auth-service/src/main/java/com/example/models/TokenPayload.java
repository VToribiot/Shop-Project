package com.example.models;

public record TokenPayload(String accessToken, String refreshToken, long expiresIn, String tokenType) {
}
