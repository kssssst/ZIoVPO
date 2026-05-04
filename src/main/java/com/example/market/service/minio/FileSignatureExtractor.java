package com.example.market.service.minio;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class FileSignatureExtractor {

    /**
     * Извлекает параметры сигнатуры из загруженного файла.
     * (Упрощённая версия – для демонстрации)
     */
    public SignatureData extract(MultipartFile file) throws IOException {
        byte[] allBytes = file.getBytes();
        int length = allBytes.length;
        // threatName – имя файла без расширения
        String originalFilename = file.getOriginalFilename();
        String threatName = originalFilename != null ? originalFilename : "unknown";
        int dotIndex = threatName.lastIndexOf('.');
        if (dotIndex > 0) threatName = threatName.substring(0, dotIndex);
        // firstBytesHex – первые 16 байт в hex
        int firstLen = Math.min(16, length);
        byte[] firstBytes = new byte[firstLen];
        System.arraycopy(allBytes, 0, firstBytes, 0, firstLen);
        String firstBytesHex = bytesToHex(firstBytes);
        // remainderHashHex – SHA-256 остатка файла (начиная с 16 байта)
        int remainderStart = firstLen;
        byte[] remainder = new byte[length - remainderStart];
        System.arraycopy(allBytes, remainderStart, remainder, 0, remainder.length);
        String remainderHashHex = sha256Hex(remainder);
        // remainderLength – длина остатка
        long remainderLength = remainder.length;
        // fileType – расширение файла
        String fileType = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileType = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
        }
        long offsetStart = 0;
        long offsetEnd = length;

        return new SignatureData(threatName, firstBytesHex, remainderHashHex,
                remainderLength, fileType, offsetStart, offsetEnd);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public record SignatureData(String threatName, String firstBytesHex,
                                String remainderHashHex, long remainderLength,
                                String fileType, long offsetStart, long offsetEnd) {}
}