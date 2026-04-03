package com.example.market.model.license;

import jakarta.persistence.*;

@Entity
@Table(name = "license_type")
public class LicenseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;               // TRIAL, MONTH, YEAR, CORPORATE
    private Integer defaultDurationInDays;
    private String description;

    public LicenseType() {}

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDefaultDurationInDays() { return defaultDurationInDays; }
    public void setDefaultDurationInDays(Integer defaultDurationInDays) { this.defaultDurationInDays = defaultDurationInDays; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}