package com.example.market.model;

public enum SessionStatus {
    ACTIVE,           // Активная сессия
    REFRESHED,        // Сессия была обновлена
    REVOKED,          // Сессия отозвана
    EXPIRED           // Сессия истекла
}