package com.example.market.binary;

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
     * Требование: реализованы сценарии бинарной выдачи.
     *
     * Полная база: exportType = 0.
     * В полную выгрузку включаются только актуальные сигнатуры, потому что клиент
     * получает снимок текущего состояния базы.
     */
    public BinaryExportData exportFull() throws IOException {
        List<MalwareSignature> signatures = signatureRepository.findByStatus(SignatureStatus.ACTUAL);
        return buildExport(signatures, (byte) 0, -1L);
    }

    /**
     * Инкремент: exportType = 1.
     * Берем все записи, у которых updatedAt больше переданного since.
     * Для инкремента since сохраняется в манифесте, чтобы клиент понимал,
     * относительно какой даты был сформирован пакет.
     */
    public BinaryExportData exportIncremental(Instant since) throws IOException {
        if (since == null) throw new IllegalArgumentException("since is required");
        List<MalwareSignature> signatures = signatureRepository.findAllUpdatedAfter(since);
        return buildExport(signatures, (byte) 1, since.toEpochMilli());
    }

    /**
     * Выгрузка по списку ID: exportType = 2.
     * Используется для точечной дозагрузки конкретных сигнатур.
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
        // Требование: реализованы данные согласно требованиям.
        //
        // Сначала формируем data.bin, потому что для манифеста нужен SHA-256 хеш
        // всего файла данных. data.bin содержит полезную нагрузку сигнатур.
        DataBuilder dataBuilder = new DataBuilder("stolnikova");
        byte[] dataBytes = dataBuilder.build(signatures);

        // Требование: манифест содержит контроль целостности data.bin.
        // SHA-256 позволяет клиенту проверить, что data.bin не был поврежден
        // или подменен при передаче.
        byte[] dataSha256 = sha256(dataBytes);

        // Требование: реализован манифест согласно требованиям.
        //
        // ManifestEntry - это индексная запись: она связывает UUID сигнатуры
        // с диапазоном байтов в data.bin, статусом, временем обновления
        // и цифровой подписью этой сигнатуры.
        List<ManifestEntry> entries = new ArrayList<>();
        long currentOffset = 0;
        // dataOffset считается относительно области записей data.bin: первая запись
        // имеет offset 0, следующая начинается после длины предыдущей записи.
        // Чтобы получить длину каждой записи, сериализуем одну сигнатуру тем же протоколом,
        // что и DataBuilder, и накапливаем currentOffset.
        for (MalwareSignature sig : signatures) {
            byte[] signatureData = serializeSingleSignature(sig);
            int length = signatureData.length;
            // statusCode - компактное бинарное представление статуса:
            // 0 = ACTUAL, 1 = DELETED. Клиент по нему понимает, добавить/обновить
            // запись или удалить ее из локальной базы.
            byte statusCode = (sig.getStatus() == SignatureStatus.ACTUAL) ? (byte) 0 : (byte) 1;

            // Подпись отдельной сигнатуры уже была рассчитана при создании/изменении.
            // В бинарном экспорте запись заново не подписываем: берем сохраненную Base64-подпись,
            // декодируем ее в байты и кладем в manifest.bin.
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

        // Заголовок манифеста хранит MAGIC NUMBER, версию, тип экспорта,
        // время формирования пакета, since, recordCount и SHA-256 data.bin.
        ManifestHeader header = new ManifestHeader(exportType, sinceEpochMillis, signatures.size(), dataSha256);

        // ManifestBuilder сериализует header + entries и подписывает готовый бинарный манифест.
        ManifestBuilder manifestBuilder = new ManifestBuilder(signingService);
        byte[] manifestBytes = manifestBuilder.build(header, entries);

        return new BinaryExportData(manifestBytes, dataBytes);
    }

    // Сериализация одной сигнатуры в формат data.bin без заголовка data.bin.
    // Этот метод нужен для вычисления dataLength и dataOffset в ManifestEntry.
    // Порядок полей должен совпадать с DataBuilder.writeSignature().
    private byte[] serializeSingleSignature(MalwareSignature sig) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        // String пишется как uint32 длина + UTF-8 байты.
        BinaryDataWriter.writeString(baos, sig.getThreatName());

        // Hex-строки из БД переводятся в сырые байты: в data.bin хранится бинарное,
        // а не текстовое hex-представление.
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
        // Протокол данных требует raw bytes. Поэтому каждые два hex-символа
        // превращаются в один байт: например "4D" -> 0x4D.
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
