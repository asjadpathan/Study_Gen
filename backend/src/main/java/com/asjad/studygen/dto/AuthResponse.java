package com.asjad.studygen.dto;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String fullName,
        String email,
        Integer currentStreak
) {
    public static AuthResponse of(String token, Long userId, String fullName, String email) {
        return new AuthResponse(token, "Bearer", userId, fullName, email, 0);
    }

    public static AuthResponse of(String token, Long userId, String fullName, String email, Integer currentStreak) {
        return new AuthResponse(token, "Bearer", userId, fullName, email, currentStreak != null ? currentStreak : 0);
    }
}