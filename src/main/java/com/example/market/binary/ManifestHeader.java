package com.example.market.binary;

/**
 * Контейнер для заголовка manifest.bin.
 * Поля соответствуют спецификации.
 *
 * Требование: реализован манифест и его заголовок с MAGIC NUMBER.
 * Заголовок идет в начале manifest.bin и позволяет клиенту понять тип пакета,
 * версию формата, сценарий экспорта и проверить целостность data.bin.
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
        // MAGIC NUMBER манифеста. MF = Manifest File, далее фамилия студента.
        // Клиент первым делом сверяет это значение.
        this.magic = "MF-stolnikova";

        // Версия бинарного формата манифеста.
        this.version = 1;

        // Тип выгрузки: 0 full, 1 increment, 2 by-ids.
        this.exportType = exportType;

        // Время создания конкретного пакета на сервере.
        this.generatedAtEpochMillis = System.currentTimeMillis();

        // Для increment хранится реальный since, для full/by-ids используется -1.
        this.sinceEpochMillis = sinceEpochMillis;

        // Количество ManifestEntry, которые клиент должен прочитать после заголовка.
        this.recordCount = recordCount;

        // SHA-256 всего data.bin для проверки целостности второй multipart-части.
        this.dataSha256 = dataSha256;
    }
}
