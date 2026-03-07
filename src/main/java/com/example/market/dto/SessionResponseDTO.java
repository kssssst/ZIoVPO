package com.example.market.dto;

import java.time.LocalDateTime;

import com.example.market.model.UserSession;

public record SessionResponseDTO(
        Long id,
        String status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        String ipAddress,
        String userAgent
) {
    public SessionResponseDTO(UserSession session) {
        this(
                session.getId(),
                session.getStatus().name(),
                session.getCreatedAt(),
                session.getExpiresAt(),
                session.getIpAddress(),
                session.getUserAgent()
        );
    }
}