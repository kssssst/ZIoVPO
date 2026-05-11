package com.example.market.signature;

import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

@Service
public class SigningService {

    private final PrivateKey privateKey;

    public SigningService(SignatureKeyStoreService keyStoreService) {
        this.privateKey = keyStoreService.getPrivateKey();
    }

    // Подпись объектного payload.
    //
    // Этот метод используется для подписи самой сигнатуры при создании/изменении:
    // объект сначала приводится к каноническому JSON, затем подписывается.
    public String sign(Object payload) {
        try {
            byte[] canonicalBytes = JsonCanonicalizer.canonicalize(payload);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(canonicalBytes);
            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign payload", e);
        }
    }

    // Требование: реализован метод, принимающий массив байт, в модуле формирования ЭЦП.
    //
    // Этот метод нужен именно для задания с manifest.bin.
    // Манифест уже является готовой бинарной последовательностью header + entries,
    // поэтому его нельзя заново канонизировать как JSON-объект.
    //
    // Принцип работы:
    // 1. Создаем алгоритм SHA256withRSA.
    // 2. Инициализируем подпись приватным ключом из keystore.
    // 3. Передаем в алгоритм исходный byte[] манифеста.
    // 4. Возвращаем сырые байты подписи, которые дописываются в конец manifest.bin.
    public byte[] signBytes(byte[] data) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign byte array", e);
        }
    }
}

