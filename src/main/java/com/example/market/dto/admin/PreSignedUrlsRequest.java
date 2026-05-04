package com.example.market.dto.admin;

import java.util.List;
import java.util.UUID;

public class PreSignedUrlsRequest {
    private List<UUID> ids;

    public List<UUID> getIds() { return ids; }
    public void setIds(List<UUID> ids) { this.ids = ids; }
}