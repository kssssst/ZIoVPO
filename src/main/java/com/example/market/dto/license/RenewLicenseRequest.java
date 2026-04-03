package com.example.market.dto.license;

import jakarta.validation.constraints.NotBlank;

public class RenewLicenseRequest {

    @NotBlank
    private String activationKey;

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }
}