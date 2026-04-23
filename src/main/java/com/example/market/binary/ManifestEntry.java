package com.example.market.binary;

import java.util.UUID;

/**
 * Одна запись в манифесте, соответствующая одной сигнатуре.
 */
public class ManifestEntry {
    public final UUID id;
    public final byte statusCode;   // 0 – ACTUAL, 1 – DELETED
    public final long updatedAtEpochMillis;
    public final long dataOffset;   // смещение начала данных в data.bin
    public final int dataLength;    // длина данных
    public final byte[] recordSignature; // подпись сигнатуры (декодирована из Base64)

    public ManifestEntry(UUID id, byte statusCode, long updatedAtEpochMillis,
                         long dataOffset, int dataLength, byte[] recordSignature) {
        this.id = id;
        this.statusCode = statusCode;
        this.updatedAtEpochMillis = updatedAtEpochMillis;
        this.dataOffset = dataOffset;
        this.dataLength = dataLength;
        this.recordSignature = recordSignature;
    }
}