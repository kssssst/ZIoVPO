package com.example.market.binary;

import com.example.market.model.malware.MalwareSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Формирует data.bin: заголовок + все сигнатуры в бинарном виде.
 *
 * Этот класс относится к заданию по бинарному API.
 * data.bin является второй частью multipart/mixed ответа и хранит полезную
 * нагрузку сигнатур без id, status, updatedAt и подписей: эти поля вынесены в manifest.bin.
 */
public class DataBuilder {

    // Требование: заголовок данных содержит MAGIC NUMBER.
    // Для data.bin используется префикс DB- + фамилия студента.
    private final String magic; // "DB-stolnikova"

    public DataBuilder(String studentLastName) {
        this.magic = "DB-" + studentLastName;
    }

    /**
     * Построить data.bin и вернуть его байты.
     *
     * Структура data.bin:
     * 1. magic ASCII: DB-stolnikova
     * 2. version: uint16
     * 3. recordCount: uint32
     * 4. recordCount бинарных записей сигнатур
     *
     * @param signatures  список сигнатур (только ACTUAL или все для инкремента)
     * @return            байты data.bin
     */
    public byte[] build(List<MalwareSignature> signatures) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // MAGIC NUMBER пишется первым, без длины. Клиент по нему проверяет,
        // что читает именно файл данных, а не манифест или ошибочный ответ.
        baos.writeBytes(magic.getBytes(StandardCharsets.US_ASCII));

        // Версия формата данных. Если структура data.bin изменится, версию можно увеличить.
        BinaryDataWriter.writeUint16(baos, 1);

        // Количество записей после заголовка. Клиент читает ровно столько записей.
        BinaryDataWriter.writeUint32(baos, signatures.size());

        // Полезная нагрузка: каждая запись сериализуется одинаковым протоколом,
        // чтобы клиент мог последовательно разобрать data.bin.
        for (MalwareSignature sig : signatures) {
            writeSignature(baos, sig);
        }
        return baos.toByteArray();
    }

    private void writeSignature(ByteArrayOutputStream out, MalwareSignature sig) throws IOException {
        // Требование: реализован протокол приведения типов файла данных
        // в бинарное представление.
        //
        // threatName: строка -> uint32 длина + UTF-8 байты.
        BinaryDataWriter.writeString(out, sig.getThreatName());

        // firstBytesHex хранится в БД как hex-строка, но в data.bin пишутся сырые байты.
        // Например "4D5A" превращается в два байта 0x4D, 0x5A.
        byte[] firstBytes = hexToBytes(sig.getFirstBytesHex());
        BinaryDataWriter.writeBytesWithLength(out, firstBytes);

        // remainderHashHex также декодируется из hex в byte[] и пишется с длиной.
        byte[] remainderHash = hexToBytes(sig.getRemainderHashHex());
        BinaryDataWriter.writeBytesWithLength(out, remainderHash);

        // Числовые long-поля пишутся как int64 в Big-Endian.
        BinaryDataWriter.writeInt64(out, sig.getRemainderLength());

        // fileType: строка -> uint32 длина + UTF-8 байты.
        BinaryDataWriter.writeString(out, sig.getFileType());

        // offsetStart и offsetEnd задают диапазон сигнатуры внутри анализируемого файла.
        BinaryDataWriter.writeInt64(out, sig.getOffsetStart());
        BinaryDataWriter.writeInt64(out, sig.getOffsetEnd());
    }

    private byte[] hexToBytes(String hex) {
        // Каждые два hex-символа кодируют один байт.
        // Character.digit(..., 16) переводит символ в значение 0..15,
        // старшая половина байта сдвигается на 4 бита и объединяется с младшей.
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
