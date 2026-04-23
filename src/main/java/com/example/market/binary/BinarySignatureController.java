package com.example.market.binary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/binary/signatures")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class BinarySignatureController {

    @Autowired
    private BinaryExportService binaryExportService;

    /**
     * Требование: реализован multipart/mixed API.
     *
     * Полная бинарная выгрузка сигнатур.
     * В ответе клиент получает не JSON, а multipart/mixed пакет из двух частей:
     * 1) manifest.bin - описание пакета, контрольные суммы, смещения и подписи;
     * 2) data.bin - сами сигнатуры в бинарном представлении.
     *
     * Для полной базы берутся только ACTUAL-записи.
     * GET /api/binary/signatures/full
     */
    @GetMapping("/full")
    public ResponseEntity<byte[]> exportFull() throws Exception {
        BinaryExportService.BinaryExportData export = binaryExportService.exportFull();
        return buildMultipartResponse(export);
    }

    /**
     * Инкрементальная бинарная выгрузка.
     *
     * since приходит в ISO-8601 формате, например 2026-01-01T00:00:00Z.
     * В exportType манифеста для такого ответа записывается код 1, а в sinceEpochMillis
     * сохраняется время since в миллисекундах Unix Epoch.
     *
     * GET /api/binary/signatures/increment?since=2026-01-01T00:00:00Z
     */
    @GetMapping("/increment")
    public ResponseEntity<byte[]> exportIncremental(@RequestParam("since") String since) throws Exception {
        Instant sinceInstant = Instant.parse(since);
        BinaryExportService.BinaryExportData export = binaryExportService.exportIncremental(sinceInstant);
        return buildMultipartResponse(export);
    }

    /**
     * Адресная бинарная выгрузка по списку UUID.
     *
     * Здесь используется POST, а не GET, потому что список UUID передается в теле
     * запроса. Если в Postman выбрать GET для этого пути, Spring вернет 405 Method Not Allowed.
     *
     * POST /api/binary/signatures/by-ids
     * Тело: {"ids":["uuid1","uuid2"]}
     */
    @PostMapping("/by-ids")
    public ResponseEntity<byte[]> exportByIds(@RequestBody IdsRequest request) throws Exception {
        List<UUID> ids = request.getIds() == null ? List.of() : request.getIds();
        BinaryExportService.BinaryExportData export = binaryExportService.exportByIds(ids);
        return buildMultipartResponse(export);
    }

    private ResponseEntity<byte[]> buildMultipartResponse(BinaryExportService.BinaryExportData export) throws Exception {
        // Требование: ответ multipart/mixed формируется корректно.
        //
        // boundary - граница между частями multipart-ответа. Сервер указывает ее
        // в общем Content-Type, а клиент по ней делит тело ответа на manifest.bin и data.bin.
        String boundary = "boundary_" + System.currentTimeMillis();

        // Первая часть ответа - manifest.bin. У части есть собственные заголовки:
        // Content-Disposition с именем файла, Content-Type как application/octet-stream
        // и Content-Length с длиной бинарного массива манифеста.
        StringBuilder headers = new StringBuilder();
        headers.append("--").append(boundary).append("\r\n");
        headers.append("Content-Disposition: attachment; filename=\"manifest.bin\"\r\n");
        headers.append("Content-Type: application/octet-stream\r\n");
        headers.append("Content-Length: ").append(export.manifest.length).append("\r\n\r\n");

        byte[] part1 = headers.toString().getBytes();

        // Тело первой части - уже готовые байты manifest.bin.
        byte[] part2 = export.manifest;

        // Вторая часть ответа - data.bin. Она идет после manifest.bin, чтобы клиент
        // сначала мог прочитать описание пакета, а затем сами бинарные данные.
        StringBuilder headers2 = new StringBuilder();
        headers2.append("\r\n--").append(boundary).append("\r\n");
        headers2.append("Content-Disposition: attachment; filename=\"data.bin\"\r\n");
        headers2.append("Content-Type: application/octet-stream\r\n");
        headers2.append("Content-Length: ").append(export.data.length).append("\r\n\r\n");

        byte[] part3 = headers2.toString().getBytes();
        byte[] part4 = export.data;
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes();

        // Собираем все части в один byte[]: это и будет тело HTTP-ответа.
        // Порядок важен для защиты: сначала manifest.bin, затем data.bin, затем закрывающий boundary.
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        baos.write(part1);
        baos.write(part2);
        baos.write(part3);
        baos.write(part4);
        baos.write(footer);

        HttpHeaders responseHeaders = new HttpHeaders();
        // Общий Content-Type ответа сообщает клиенту, что внутри несколько частей,
        // и передает boundary для их разбора.
        responseHeaders.setContentType(MediaType.parseMediaType("multipart/mixed; boundary=" + boundary));

        return ResponseEntity.ok().headers(responseHeaders).body(baos.toByteArray());
    }

    static class IdsRequest {
        private List<UUID> ids;
        public List<UUID> getIds() { return ids; }
        public void setIds(List<UUID> ids) { this.ids = ids; }
    }
}
