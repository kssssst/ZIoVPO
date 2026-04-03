package com.example.market.dto.license;

import java.time.LocalDate;

public class LicenseResponse {

    private Long id;
    private String code;
    private Long productId;
    private String productName;
    private Long typeId;
    private String typeName;
    private Long ownerId;
    private String ownerEmail;
    private Long userId;
    private String userEmail;
    private LocalDate firstActivationDate;
    private LocalDate endingDate;
    private Boolean blocked;
    private Integer deviceCount;

    public LicenseResponse(Long id, String code, Long productId, String productName,
                           Long typeId, String typeName, Long ownerId, String ownerEmail,
                           Long userId, String userEmail, LocalDate firstActivationDate,
                           LocalDate endingDate, Boolean blocked, Integer deviceCount) {
        this.id = id;
        this.code = code;
        this.productId = productId;
        this.productName = productName;
        this.typeId = typeId;
        this.typeName = typeName;
        this.ownerId = ownerId;
        this.ownerEmail = ownerEmail;
        this.userId = userId;
        this.userEmail = userEmail;
        this.firstActivationDate = firstActivationDate;
        this.endingDate = endingDate;
        this.blocked = blocked;
        this.deviceCount = deviceCount;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Long getTypeId() { return typeId; }
    public String getTypeName() { return typeName; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerEmail() { return ownerEmail; }
    public Long getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public LocalDate getFirstActivationDate() { return firstActivationDate; }
    public LocalDate getEndingDate() { return endingDate; }
    public Boolean getBlocked() { return blocked; }
    public Integer getDeviceCount() { return deviceCount; }
}