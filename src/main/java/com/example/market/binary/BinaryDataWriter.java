package com.example.market.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Утилита для записи примитивных типов в массив байт (Big‑Endian / network byte order).
 *
 * Требование: реализован протокол приведения типов файла данных
 * в бинарное представление.
 *
 * Все многобайтовые числа пишутся в Big-Endian: сначала старший байт,
 * затем младшие. Такой порядок также называют network byte order.
 * Благодаря этому клиент и сервер одинаково читают длины, смещения,
 * времена и UUID независимо от платформы.
 *
 * Используется для формирования manifest.bin и data.bin.
 */
public final class BinaryDataWriter {

    private BinaryDataWriter() {}

    // uint16: 2 байта, старший байт пишется первым.
    // value >>> 8 берет старшие 8 бит, value & 0xFF берет младшие 8 бит.
    public static void writeUint16(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    // uint32: 4 байта в порядке 24, 16, 8, 0 бит.
    // Так пишутся длины строк, длины byte[] и recordCount.
    public static void writeUint32(ByteArrayOutputStream out, long value) {
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    // int64: 8 байт в порядке 56, 48, 40, 32, 24, 16, 8, 0 бит.
    // Используется для времени, offsetStart, offsetEnd и частей UUID.
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

    // byte[] переменной длины: сначала uint32 длина, затем сами байты.
    // Клиент сначала читает длину, затем ровно столько байт содержимого.
    public static void writeBytesWithLength(ByteArrayOutputStream out, byte[] data) {
        writeUint32(out, data.length);
        out.writeBytes(data);
    }

    // String: переводим строку в UTF-8, затем пишем как byte[] с длиной.
    // Поэтому русские/латинские символы читаются одинаково корректно.
    public static void writeString(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeBytesWithLength(out, bytes);
    }

    // UUID пишется компактно не строкой, а двумя 64-битными числами:
    // mostSignificantBits и leastSignificantBits.
    public static void writeUuid(ByteArrayOutputStream out, UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        writeInt64(out, most);
        writeInt64(out, least);
    }
}
