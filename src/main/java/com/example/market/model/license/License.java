package com.example.market.model.license;

import com.example.market.model.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "license")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;   // activation key

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private LicenseType type;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;            // who bought it

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;             // who activated it (null before first activation)

    private LocalDate firstActivationDate;
    private LocalDate endingDate;
    private Boolean blocked = false;
    private Integer deviceCount = 1;   // max number of devices (default 1)
    private String description;

    public License() {}

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public LicenseType getType() { return type; }
    public void setType(LicenseType type) { this.type = type; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDate getFirstActivationDate() { return firstActivationDate; }
    public void setFirstActivationDate(LocalDate firstActivationDate) { this.firstActivationDate = firstActivationDate; }
    public LocalDate getEndingDate() { return endingDate; }
    public void setEndingDate(LocalDate endingDate) { this.endingDate = endingDate; }
    public Boolean getBlocked() { return blocked; }
    public void setBlocked(Boolean blocked) { this.blocked = blocked; }
    public Integer getDeviceCount() { return deviceCount; }
    public void setDeviceCount(Integer deviceCount) { this.deviceCount = deviceCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}