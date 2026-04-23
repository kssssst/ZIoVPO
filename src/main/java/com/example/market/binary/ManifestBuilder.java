package com.example.market.binary;

import com.example.market.signature.SigningService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Собирает бинарный манифест: заголовок + массив записей + подпись манифеста.
 *
 * Этот класс относится к заданию по multipart/mixed API: manifest.bin является
 * первой частью ответа и описывает, как читать вторую часть data.bin.
 */
public class ManifestBuilder {

    private final SigningService signingService;

    public ManifestBuilder(SigningService signingService) {
        this.signingService = signingService;
    }

    /**
     * Построить manifest.bin массив байт.
     *
     * Принцип:
     * 1. Записываем заголовок манифеста.
     * 2. Записываем ровно recordCount записей ManifestEntry.
     * 3. Берем получившийся неподписанный byte[] и подписываем его через signBytes().
     * 4. В конец дописываем длину подписи и байты подписи.
     *
     * @param header        заголовок манифеста
     * @param entries       список записей (ManifestEntry)
     * @return              байты manifest.bin
     */
    public byte[] build(ManifestHeader header, List<ManifestEntry> entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Требование: заголовок манифеста содержит MAGIC NUMBER.
        // magic пишется первым и без префикса длины, чтобы клиент сразу понял,
        // что перед ним именно manifest.bin этого протокола.
        baos.writeBytes(header.magic.getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        // version нужен для развития формата. Сейчас версия = 1.
        BinaryDataWriter.writeUint16(baos, header.version);

        // exportType: 0 - full, 1 - increment, 2 - by-ids.
        baos.write(header.exportType);

        // generatedAtEpochMillis - время формирования пакета сервером.
        BinaryDataWriter.writeInt64(baos, header.generatedAtEpochMillis);

        // sinceEpochMillis используется для инкремента; для остальных сценариев -1.
        BinaryDataWriter.writeInt64(baos, header.sinceEpochMillis);

        // recordCount сообщает клиенту, сколько ManifestEntry нужно прочитать дальше.
        BinaryDataWriter.writeUint32(baos, header.recordCount);

        // dataSha256 - контрольная сумма всего data.bin.
        baos.writeBytes(header.dataSha256);

        // Массив записей манифеста. Каждая запись связывает одну сигнатуру
        // с ее байтовым диапазоном в data.bin и содержит подпись этой сигнатуры.
        for (ManifestEntry entry : entries) {
            // id пишется как два int64: mostSignificantBits и leastSignificantBits.
            BinaryDataWriter.writeUuid(baos, entry.id);
            // statusCode: 0 = ACTUAL, 1 = DELETED.
            baos.write(entry.statusCode);
            // updatedAtEpochMillis нужен клиенту для сравнения версии записи.
            BinaryDataWriter.writeInt64(baos, entry.updatedAtEpochMillis);
            // dataOffset и dataLength задают диапазон байтов конкретной записи в data.bin.
            BinaryDataWriter.writeInt64(baos, entry.dataOffset);
            BinaryDataWriter.writeUint32(baos, entry.dataLength);
            // Подпись записи имеет переменную длину, поэтому сначала пишем uint32 length.
            BinaryDataWriter.writeUint32(baos, entry.recordSignature.length);
            baos.writeBytes(entry.recordSignature);
        }

        // Требование: корректно реализована ЭЦП манифеста.
        //
        // Подписывается не объект и не JSON, а именно готовая бинарная структура
        // header + entries. Поэтому сначала получаем unsignedManifest как byte[].
        byte[] unsignedManifest = baos.toByteArray();

        // Требование: в модуле ЭЦП есть метод, принимающий массив байт.
        // signBytes(byte[]) возвращает сырые байты RSA-подписи для manifest.bin.
        byte[] manifestSignature = signingService.signBytes(unsignedManifest);

        ByteArrayOutputStream finalBaos = new ByteArrayOutputStream();
        finalBaos.writeBytes(unsignedManifest);
        // В конец manifest.bin кладем длину подписи и саму подпись.
        // Клиент читает unsigned-часть, затем подпись и проверяет подлинность манифеста.
        BinaryDataWriter.writeUint32(finalBaos, manifestSignature.length);
        finalBaos.writeBytes(manifestSignature);
        return finalBaos.toByteArray();
    }
}
