package com.example.market.dto.license;

import jakarta.validation.constraints.NotBlank;

public class ActivateLicenseRequest {

    @NotBlank
    private String activationKey;

    @NotBlank
    private String deviceMac;

    private String deviceName;

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
