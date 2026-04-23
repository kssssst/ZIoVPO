package com.example.market.binary;

import com.example.market.signature.SigningService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Собирает бинарный манифест: заголовок + массив записей + подпись манифеста.
 */
public class ManifestBuilder {

    private final SigningService signingService;

    public ManifestBuilder(SigningService signingService) {
        this.signingService = signingService;
    }

    /**
     * Построить manifest.bin массив байт.
     * @param header        заголовок манифеста
     * @param entries       список записей (ManifestEntry)
     * @return              байты manifest.bin
     */
    public byte[] build(ManifestHeader header, List<ManifestEntry> entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 1. magic (как строка) – без префикса длины, просто байты
        baos.writeBytes(header.magic.getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        // 2. version (uint16)
        BinaryDataWriter.writeUint16(baos, header.version);

        // 3. exportType (uint8)
        baos.write(header.exportType);

        // 4. generatedAtEpochMillis (int64)
        BinaryDataWriter.writeInt64(baos, header.generatedAtEpochMillis);

        // 5. sinceEpochMillis (int64)
        BinaryDataWriter.writeInt64(baos, header.sinceEpochMillis);

        // 6. recordCount (uint32)
        BinaryDataWriter.writeUint32(baos, header.recordCount);

        // 7. dataSha256 (32 байта)
        baos.writeBytes(header.dataSha256);

        // 8. Массив записей
        for (ManifestEntry entry : entries) {
            // id
            BinaryDataWriter.writeUuid(baos, entry.id);
            // statusCode
            baos.write(entry.statusCode);
            // updatedAtEpochMillis
            BinaryDataWriter.writeInt64(baos, entry.updatedAtEpochMillis);
            // dataOffset
            BinaryDataWriter.writeInt64(baos, entry.dataOffset);
            // dataLength
            BinaryDataWriter.writeUint32(baos, entry.dataLength);
            // recordSignatureLength (uint32)
            BinaryDataWriter.writeUint32(baos, entry.recordSignature.length);
            // recordSignatureBytes
            baos.writeBytes(entry.recordSignature);
        }

        // 9. После записей – подпись манифеста: сначала длина (uint32), потом байты подписи
        byte[] unsignedManifest = baos.toByteArray();
        // ИСПРАВЛЕНО: используем signBytes(), возвращающий byte[]
        byte[] manifestSignature = signingService.signBytes(unsignedManifest);

        ByteArrayOutputStream finalBaos = new ByteArrayOutputStream();
        finalBaos.writeBytes(unsignedManifest);
        BinaryDataWriter.writeUint32(finalBaos, manifestSignature.length);
        finalBaos.writeBytes(manifestSignature);
        return finalBaos.toByteArray();
    }
}