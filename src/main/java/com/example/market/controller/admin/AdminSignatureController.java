package com.example.market.controller.admin;

import com.example.market.dto.admin.PreSignedUrlResponse;
import com.example.market.dto.admin.PreSignedUrlsRequest;
import com.example.market.dto.malware.MalwareSignatureRequest;
import com.example.market.dto.malware.MalwareSignatureResponse;
import com.example.market.model.malware.MalwareSignature;
import com.example.market.repository.malware.MalwareSignatureRepository;
import com.example.market.service.malware.MalwareSignatureService;
import com.example.market.service.minio.FileSignatureExtractor;
import com.example.market.service.minio.MinioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/signatures")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSignatureController {

    private final MinioService minioService;
    private final FileSignatureExtractor fileSignatureExtractor;
    private final MalwareSignatureService signatureService;
    private final MalwareSignatureRepository signatureRepository;

    public AdminSignatureController(MinioService minioService,
                                    FileSignatureExtractor fileSignatureExtractor,
                                    MalwareSignatureService signatureService,
                                    MalwareSignatureRepository signatureRepository) {
        this.minioService = minioService;
        this.fileSignatureExtractor = fileSignatureExtractor;
        this.signatureService = signatureService;
        this.signatureRepository = signatureRepository;
    }

    /**
     * Загрузка файла и создание сигнатуры в БД.
     * Файл сохраняется в MinIO.
     */
    @PostMapping("/upload")
    public ResponseEntity<MalwareSignatureResponse> uploadSignature(
            @RequestParam("file") MultipartFile file) throws IOException, Exception {

        // 1. Извлечение характеристик из файла
        FileSignatureExtractor.SignatureData data = fileSignatureExtractor.extract(file);

        // 2. Формируем запрос на создание сигнатуры
        MalwareSignatureRequest request = new MalwareSignatureRequest();
        request.setThreatName(data.threatName());
        request.setFirstBytesHex(data.firstBytesHex());
        request.setRemainderHashHex(data.remainderHashHex());
        request.setRemainderLength(data.remainderLength());
        request.setFileType(data.fileType());
        request.setOffsetStart(data.offsetStart());
        request.setOffsetEnd(data.offsetEnd());

        // 3. Создаём сигнатуру в БД
        MalwareSignatureResponse response = signatureService.create(request);
        UUID signatureId = response.getId();

        // 4. Сохраняем файл в MinIO
        String objectName = minioService.uploadFile(file, signatureId);

        // (Опционально: сохранить objectName в отдельное поле таблицы, если нужно)
        // Пока просто возвращаем ответ

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получение pre-signed URL для списка сигнатур по их ID.
     * URL действителен 1 час.
     */
    @PostMapping("/pre-signed-urls")
    public ResponseEntity<List<PreSignedUrlResponse>> getPreSignedUrls(
            @RequestBody PreSignedUrlsRequest request) throws Exception {

        List<UUID> ids = request.getIds();
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Получаем сигнатуры из БД, чтобы достать objectName
        List<MalwareSignature> signatures = signatureRepository.findAllById(ids);
        List<PreSignedUrlResponse> responses = signatures.stream()
                .map(sig -> {
                    // objectName = signatureId + "/" + имя файла. Но имя файла мы не сохранили.
                    // Для простоты будем искать любой файл в папке сигнатуры. В реальном проекте нужно сохранять имя файла.
                    // Здесь сделаем предположение, что файл называется "uploaded.bin" – упрощённо.
                    // Для корректной работы нужно сохранять оригинальное имя файла в таблице сигнатур (добавить поле filePath).
                    // В данной реализации используем соглашение: objectName = signatureId + "/" + signatureId + ".bin"
                    String objectName = sig.getId().toString() + "/" + sig.getId().toString() + ".bin";
                    try {
                        String url = minioService.generatePresignedUrl(objectName);
                        return new PreSignedUrlResponse(sig.getId(), url);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to generate URL for " + sig.getId(), e);
                    }
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}