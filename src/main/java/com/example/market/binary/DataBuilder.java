package com.example.market.binary;

import com.example.market.model.malware.MalwareSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Формирует data.bin: заголовок + все сигнатуры в бинарном виде.
 */
public class DataBuilder {

    private final String magic; // "DB-stolnikova"

    public DataBuilder(String studentLastName) {
        this.magic = "DB-" + studentLastName;
    }

    /**
     * Построить data.bin и вернуть его байты.
     * @param signatures  список сигнатур (только ACTUAL или все для инкремента)
     * @return            байты data.bin
     */
    public byte[] build(List<MalwareSignature> signatures) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Заголовок data.bin:
        // - magic (ASCII)
        baos.writeBytes(magic.getBytes(StandardCharsets.US_ASCII));
        // - version (uint16) = 1
        BinaryDataWriter.writeUint16(baos, 1);
        // - recordCount (uint32)
        BinaryDataWriter.writeUint32(baos, signatures.size());

        // Полезная нагрузка: каждая запись – сериализованная сигнатура
        for (MalwareSignature sig : signatures) {
            writeSignature(baos, sig);
        }
        return baos.toByteArray();
    }

    private void writeSignature(ByteArrayOutputStream out, MalwareSignature sig) throws IOException {
        // threatName (строка)
        BinaryDataWriter.writeString(out, sig.getThreatName());
        // firstBytes (сырые байты из hex)
        byte[] firstBytes = hexToBytes(sig.getFirstBytesHex());
        BinaryDataWriter.writeBytesWithLength(out, firstBytes);
        // remainderHash (сырые байты из hex)
        byte[] remainderHash = hexToBytes(sig.getRemainderHashHex());
        BinaryDataWriter.writeBytesWithLength(out, remainderHash);
        // remainderLength (int64)
        BinaryDataWriter.writeInt64(out, sig.getRemainderLength());
        // fileType (строка)
        BinaryDataWriter.writeString(out, sig.getFileType());
        // offsetStart (int64)
        BinaryDataWriter.writeInt64(out, sig.getOffsetStart());
        // offsetEnd (int64)
        BinaryDataWriter.writeInt64(out, sig.getOffsetEnd());
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
}