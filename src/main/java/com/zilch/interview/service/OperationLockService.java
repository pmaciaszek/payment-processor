package com.zilch.interview.service;

import com.zilch.interview.config.properties.OperationLockServiceProperties;
import com.zilch.interview.model.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class OperationLockService {

    private final ConcurrentHashMap<String, PaymentResult> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> retryCounts = new ConcurrentHashMap<>();

    private final OperationLockServiceProperties operationLockServiceProperties;

    public PaymentResult execute(String idempotencyKey, Supplier<PaymentResult> supplier) {
        return getCachedResult(idempotencyKey)
                .filter(cached -> cached.success() || isRetryLimitReached(idempotencyKey))
                .orElseGet(() -> processNewAction(idempotencyKey, supplier));
    }

    private Optional<PaymentResult> getCachedResult(String idempotencyKey) {
        return Optional.ofNullable(cache.get(idempotencyKey));
    }

    private boolean isRetryLimitReached(String idempotencyKey) {
        return retryCounts.getOrDefault(idempotencyKey, 0) >= operationLockServiceProperties.maxRetries();
    }

    private PaymentResult processNewAction(String idempotencyKey, Supplier<PaymentResult> supplier) {
        var lock = getLock(idempotencyKey);
        lock.lock();
        try {
            return getCachedResult(idempotencyKey)
                    .filter(cached -> cached.success() || isRetryLimitReached(idempotencyKey))
                    .orElseGet(() -> performSupplierAction(idempotencyKey, supplier));
        } finally {
            lock.unlock();
        }
    }

    private PaymentResult performSupplierAction(String idempotencyKey, Supplier<PaymentResult> supplier) {
        retryCounts.merge(idempotencyKey, 1, Integer::sum);
        var result = supplier.get();
        cache.put(idempotencyKey, result);
        return result;
    }

    private ReentrantLock getLock(String idempotencyKey) {
        return locks.computeIfAbsent(idempotencyKey, key -> new ReentrantLock());
    }
}
