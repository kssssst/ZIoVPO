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

    public String sign(Object payload) {
        try {
            // 1. Канонизация в UTF-8 байты
            byte[] canonicalBytes = JsonCanonicalizer.canonicalize(payload);

            // 2. Подпись SHA256withRSA
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(canonicalBytes);
            byte[] signedBytes = signature.sign();

            // 3. Base64 кодирование
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign payload", e);
        }
    }
}