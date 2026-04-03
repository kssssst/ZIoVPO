package com.example.market.dto.license;

import java.time.LocalDateTime;
import java.time.LocalDate;

public class Ticket {

    private LocalDateTime serverTime;
    private Long ttlMillis;
    private LocalDate activationDate;
    private LocalDate expirationDate;
    private Long userId;
    private String deviceMac;
    private Boolean blocked;

    public Ticket(LocalDateTime serverTime, Long ttlMillis, LocalDate activationDate,
                  LocalDate expirationDate, Long userId, String deviceMac, Boolean blocked) {
        this.serverTime = serverTime;
        this.ttlMillis = ttlMillis;
        this.activationDate = activationDate;
        this.expirationDate = expirationDate;
        this.userId = userId;
        this.deviceMac = deviceMac;
        this.blocked = blocked;
    }

    public LocalDateTime getServerTime() { return serverTime; }
    public Long getTtlMillis() { return ttlMillis; }
    public LocalDate getActivationDate() { return activationDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public Long getUserId() { return userId; }
    public String getDeviceMac() { return deviceMac; }
    public Boolean getBlocked() { return blocked; }
}