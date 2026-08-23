package com.baraseck.supportflow.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {}
