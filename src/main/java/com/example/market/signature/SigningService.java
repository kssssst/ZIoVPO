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

    // Подпись объекта (JSON канонизация)
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

    // Подпись готового массива байт
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