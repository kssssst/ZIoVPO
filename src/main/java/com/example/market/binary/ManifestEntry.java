package com.example.market.binary;

import java.util.UUID;

/**
 * Одна запись в манифесте, соответствующая одной сигнатуре.
 *
 * ManifestEntry не хранит сами поля сигнатуры. Он хранит индексную информацию:
 * где запись находится в data.bin, какой у нее статус и какая у нее подпись.
 * Клиент читает recordCount из ManifestHeader и затем разбирает ровно столько
 * ManifestEntry подряд.
 */
public class ManifestEntry {
    public final UUID id;
    public final byte statusCode;   // 0 – ACTUAL, 1 – DELETED
    public final long updatedAtEpochMillis;
    public final long dataOffset;   // смещение начала записи в области полезной нагрузки data.bin
    public final int dataLength;    // длина бинарной записи в data.bin
    public final byte[] recordSignature; // подпись сигнатуры, декодированная из digitalSignatureBase64

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
