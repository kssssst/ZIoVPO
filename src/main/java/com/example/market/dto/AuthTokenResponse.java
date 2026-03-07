package com.example.market.dto;

import java.util.Date;

import com.example.market.service.TokenService;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        Date accessTokenExpiry,
        Date refreshTokenExpiry,
        String tokenType
) {
    public static AuthTokenResponse from(TokenService.TokenPair tokenPair) {
        return new AuthTokenResponse(
                tokenPair.getAccessToken(),
                tokenPair.getRefreshToken(),
                tokenPair.getAccessTokenExpiry(),
                tokenPair.getRefreshTokenExpiry(),
                "Bearer"
        );
    }
}