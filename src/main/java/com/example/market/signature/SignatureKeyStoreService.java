package com.example.market.signature;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

@Service
public class SignatureKeyStoreService {

    private final SignatureProperties properties;
    private PrivateKey privateKey;
    private Certificate certificate;

    public SignatureKeyStoreService(SignatureProperties properties) {
        this.properties = properties;
        loadKeys();
    }

    private void loadKeys() {
        try {
            Resource resource;
            String path = properties.getKeyStorePath();
            if (path.startsWith("classpath:")) {
                resource = new ClassPathResource(path.substring("classpath:".length()));
            } else if (path.startsWith("file:")) {
                resource = new FileSystemResource(path.substring("file:".length()));
            } else {
                resource = new FileSystemResource(path);
            }

            KeyStore keyStore = KeyStore.getInstance(properties.getKeyStoreType());
            try (InputStream is = resource.getInputStream()) {
                keyStore.load(is, properties.getKeyStorePassword().toCharArray());
            }

            String alias = properties.getKeyAlias();
            String keyPassword = properties.getKeyPassword() != null ?
                    properties.getKeyPassword() : properties.getKeyStorePassword();

            privateKey = (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
            certificate = keyStore.getCertificate(alias);

            if (privateKey == null) {
                throw new RuntimeException("Private key not found for alias: " + alias);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load signature keystore", e);
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public Certificate getCertificate() {
        return certificate;
    }
}