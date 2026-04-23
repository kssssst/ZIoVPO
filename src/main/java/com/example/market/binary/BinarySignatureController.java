package com.example.market.controller.binary;

import com.example.market.service.binary.BinaryExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
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
     * Полная база сигнатур (только ACTUAL)
     * GET /api/binary/signatures/full
     */
    @GetMapping("/full")
    public ResponseEntity<byte[]> exportFull() throws Exception {
        BinaryExportService.BinaryExportData export = binaryExportService.exportFull();
        return buildMultipartResponse(export);
    }

    /**
     * Инкремент
     * GET /api/binary/signatures/increment?since=2026-01-01T00:00:00Z
     */
    @GetMapping("/increment")
    public ResponseEntity<byte[]> exportIncremental(@RequestParam("since") String since) throws Exception {
        Instant sinceInstant = Instant.parse(since);
        BinaryExportService.BinaryExportData export = binaryExportService.exportIncremental(sinceInstant);
        return buildMultipartResponse(export);
    }

    /**
     * По списку ID (POST)
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
        String boundary = "boundary_" + System.currentTimeMillis();

        // Формируем multipart/mixed тело
        StringBuilder headers = new StringBuilder();
        headers.append("--").append(boundary).append("\r\n");
        headers.append("Content-Disposition: attachment; filename=\"manifest.bin\"\r\n");
        headers.append("Content-Type: application/octet-stream\r\n");
        headers.append("Content-Length: ").append(export.manifest.length).append("\r\n\r\n");

        byte[] part1 = headers.toString().getBytes();

        byte[] part2 = export.manifest;

        StringBuilder headers2 = new StringBuilder();
        headers2.append("\r\n--").append(boundary).append("\r\n");
        headers2.append("Content-Disposition: attachment; filename=\"data.bin\"\r\n");
        headers2.append("Content-Type: application/octet-stream\r\n");
        headers2.append("Content-Length: ").append(export.data.length).append("\r\n\r\n");

        byte[] part3 = headers2.toString().getBytes();
        byte[] part4 = export.data;
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes();

        // Собираем всё в один массив
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        baos.write(part1);
        baos.write(part2);
        baos.write(part3);
        baos.write(part4);
        baos.write(footer);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.parseMediaType("multipart/mixed; boundary=" + boundary));

        return ResponseEntity.ok().headers(responseHeaders).body(baos.toByteArray());
    }

    static class IdsRequest {
        private List<UUID> ids;
        public List<UUID> getIds() { return ids; }
        public void setIds(List<UUID> ids) { this.ids = ids; }
    }
}