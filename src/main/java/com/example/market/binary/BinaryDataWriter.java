package com.example.market.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Утилита для записи примитивных типов в массив байт (Big‑Endian / network byte order).
 * Используется для формирования manifesto.bin и data.bin.
 */
public final class BinaryDataWriter {

    private BinaryDataWriter() {}

    // Запись uint16 (2 байта)
    public static void writeUint16(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    // Запись uint32 (4 байта)
    public static void writeUint32(ByteArrayOutputStream out, long value) {
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    // Запись int64 (8 байт)
    public static void writeInt64(ByteArrayOutputStream out, long value) {
        out.write((int) ((value >>> 56) & 0xFF));
        out.write((int) ((value >>> 48) & 0xFF));
        out.write((int) ((value >>> 40) & 0xFF));
        out.write((int) ((value >>> 32) & 0xFF));
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    // Запись массива байт с префиксом длины (uint32)
    public static void writeBytesWithLength(ByteArrayOutputStream out, byte[] data) {
        writeUint32(out, data.length);
        out.writeBytes(data);
    }

    // Запись строки UTF‑8 с префиксом длины (uint32)
    public static void writeString(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeBytesWithLength(out, bytes);
    }

    // Запись UUID (как два 64-битных поля)
    public static void writeUuid(ByteArrayOutputStream out, UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        writeInt64(out, most);
        writeInt64(out, least);
    }
}