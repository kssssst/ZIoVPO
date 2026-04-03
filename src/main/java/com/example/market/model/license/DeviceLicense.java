package com.example.market.model.license;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_license")
public class DeviceLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "license_id", nullable = false)
    private License license;

    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    private LocalDateTime activationDate;

    public DeviceLicense() {}

    public DeviceLicense(License license, Device device) {
        this.license = license;
        this.device = device;
        this.activationDate = LocalDateTime.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public License getLicense() { return license; }
    public void setLicense(License license) { this.license = license; }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }
    public LocalDateTime getActivationDate() { return activationDate; }
    public void setActivationDate(LocalDateTime activationDate) { this.activationDate = activationDate; }
}
