package com.example.market.dto.admin;

import java.util.UUID;

public class PreSignedUrlResponse {
    private UUID signatureId;
    private String url;

    public PreSignedUrlResponse(UUID signatureId, String url) {
        this.signatureId = signatureId;
        this.url = url;
    }

    public UUID getSignatureId() { return signatureId; }
    public String getUrl() { return url; }
}