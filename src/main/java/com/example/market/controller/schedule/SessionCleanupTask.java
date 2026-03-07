package com.example.market.schedule;

import com.example.market.service.TokenService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionCleanupTask {

    private final TokenService tokenService;

    public SessionCleanupTask(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    // Запускаем каждый день в 3:00 ночи
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredSessions() {
        tokenService.cleanupExpiredSessions();
    }
}