package com.example.market.service.binary;

import com.example.market.binary.*;
import com.example.market.model.malware.MalwareSignature;
import com.example.market.model.malware.SignatureStatus;
import com.example.market.repository.malware.MalwareSignatureRepository;
import com.example.market.signature.SigningService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class BinaryExportService {

    private final MalwareSignatureRepository signatureRepository;
    private final SigningService signingService;

    public BinaryExportService(MalwareSignatureRepository signatureRepository,
                               SigningService signingService) {
        this.signatureRepository = signatureRepository;
        this.signingService = signingService;
    }

    /**
     * Полная база (только ACTUAL)
     */
    public BinaryExportData exportFull() throws IOException {
        List<MalwareSignature> signatures = signatureRepository.findByStatus(SignatureStatus.ACTUAL);
        return buildExport(signatures, (byte) 0, -1L);
    }

    /**
     * Инкремент (updatedAt > since)
     */
    public BinaryExportData exportIncremental(Instant since) throws IOException {
        if (since == null) throw new IllegalArgumentException("since is required");
        List<MalwareSignature> signatures = signatureRepository.findAllUpdatedAfter(since);
        return buildExport(signatures, (byte) 1, since.toEpochMilli());
    }

    /**
     * По списку ID
     */
    public BinaryExportData exportByIds(List<UUID> ids) throws IOException {
        if (ids == null || ids.isEmpty()) {
            // пустой экспорт
            return buildExport(new ArrayList<>(), (byte) 2, -1L);
        }
        List<MalwareSignature> signatures = signatureRepository.findByIdIn(ids);
        return buildExport(signatures, (byte) 2, -1L);
    }

    private BinaryExportData buildExport(List<MalwareSignature> signatures, byte exportType, long sinceEpochMillis) throws IOException {
        // Собираем data.bin
        DataBuilder dataBuilder = new DataBuilder("stolnikova");
        byte[] dataBytes = dataBuilder.build(signatures);
        // Вычисляем SHA‑256 data.bin
        byte[] dataSha256 = sha256(dataBytes);

        // Строим записи манифеста и одновременно заполняем информацию о смещениях
        List<ManifestEntry> entries = new ArrayList<>();
        long currentOffset = 0;
        // Для вычисления смещений нужно знать, как именно сериализуется каждая сигнатура в data.bin.
        // Можно пройтись по списку ещё раз, эмулируя сериализацию. Или сериализовать в поток и получить смещения.
        // Проще: сериализовать каждую сигнатуру отдельно, считать её длину и накопить смещения.
        for (MalwareSignature sig : signatures) {
            byte[] signatureData = serializeSingleSignature(sig);
            int length = signatureData.length;
            // Статус код: 0 для ACTUAL, 1 для DELETED
            byte statusCode = (sig.getStatus() == SignatureStatus.ACTUAL) ? (byte) 0 : (byte) 1;
            // Подпись сигнатуры (декодируем из Base64)
            byte[] recordSig = Base64.getDecoder().decode(sig.getDigitalSignatureBase64());
            ManifestEntry entry = new ManifestEntry(
                    sig.getId(),
                    statusCode,
                    sig.getUpdatedAt().toEpochMilli(),
                    currentOffset,
                    length,
                    recordSig
            );
            entries.add(entry);
            currentOffset += length;
        }

        ManifestHeader header = new ManifestHeader(exportType, sinceEpochMillis, signatures.size(), dataSha256);
        ManifestBuilder manifestBuilder = new ManifestBuilder(signingService);
        byte[] manifestBytes = manifestBuilder.build(header, entries);

        return new BinaryExportData(manifestBytes, dataBytes);
    }

    // Сериализация одной сигнатуры в формат data.bin (без заголовка data.bin)
    private byte[] serializeSingleSignature(MalwareSignature sig) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        // Методы из DataBuilder (можно использовать тот же экземпляр, но проще повторить логику)
        BinaryDataWriter.writeString(baos, sig.getThreatName());
        byte[] firstBytes = hexToBytes(sig.getFirstBytesHex());
        BinaryDataWriter.writeBytesWithLength(baos, firstBytes);
        byte[] remainderHash = hexToBytes(sig.getRemainderHashHex());
        BinaryDataWriter.writeBytesWithLength(baos, remainderHash);
        BinaryDataWriter.writeInt64(baos, sig.getRemainderLength());
        BinaryDataWriter.writeString(baos, sig.getFileType());
        BinaryDataWriter.writeInt64(baos, sig.getOffsetStart());
        BinaryDataWriter.writeInt64(baos, sig.getOffsetEnd());
        return baos.toByteArray();
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static class BinaryExportData {
        public final byte[] manifest;
        public final byte[] data;
        public BinaryExportData(byte[] manifest, byte[] data) {
            this.manifest = manifest;
            this.data = data;
        }
    }
}