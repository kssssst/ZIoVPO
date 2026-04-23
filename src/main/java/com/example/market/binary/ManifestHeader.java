package com.example.market.binary;

/**
 * Контейнер для заголовка manifest.bin.
 * Поля соответствуют спецификации.
 */
public class ManifestHeader {
    /** magic – сигнатура формата: "MF-stolnikova" */
    public final String magic;
    /** version = 1 */
    public final short version;
    /** exportType: 0 – полная база, 1 – инкремент, 2 – по списку ID */
    public final byte exportType;
    /** Время создания пакета в миллисекундах (Unix epoch) */
    public final long generatedAtEpochMillis;
    /** Параметр since (для инкремента), для остальных – -1 */
    public final long sinceEpochMillis;
    /** Количество сигнатур в пакете */
    public final int recordCount;
    /** SHA‑256 хеш data.bin (32 байта) */
    public final byte[] dataSha256;

    public ManifestHeader(byte exportType, long sinceEpochMillis, int recordCount, byte[] dataSha256) {
        this.magic = "MF-stolnikova";
        this.version = 1;
        this.exportType = exportType;
        this.generatedAtEpochMillis = System.currentTimeMillis();
        this.sinceEpochMillis = sinceEpochMillis;
        this.recordCount = recordCount;
        this.dataSha256 = dataSha256;
    }
}