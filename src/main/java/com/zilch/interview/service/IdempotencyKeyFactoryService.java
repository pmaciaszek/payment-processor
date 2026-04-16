package com.zilch.interview.service;

import com.zilch.interview.exception.RequestBodyHashCreationException;
import com.zilch.interview.model.IdempotencyKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class IdempotencyKeyFactoryService {

    private static final ThreadLocal<MessageDigest> SHA256_DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    });

    private final ObjectMapper objectMapper;

    public IdempotencyKey createIdempotencyKey(String requestId, Object requestBody) {
        return new IdempotencyKey(requestId, hashRequestBody(requestBody));
    }

    private String hashRequestBody(Object requestBody) {
        try {
            var digest = SHA256_DIGEST.get();
            digest.reset();

            return HexFormat.of()
                    .formatHex(digest
                            .digest(objectMapper.writeValueAsString(requestBody)
                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RequestBodyHashCreationException(e);
        }
    }
}
