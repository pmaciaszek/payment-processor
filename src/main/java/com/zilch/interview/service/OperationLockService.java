package com.zilch.interview.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zilch.interview.config.properties.ServicesProperties;
import com.zilch.interview.exception.IdempotencyKeyDuplicationException;
import com.zilch.interview.model.CacheEntry;
import com.zilch.interview.model.IdempotencyKey;
import com.zilch.interview.model.PaymentResult;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class OperationLockService {

    private final Cache<String, CacheEntry> cache;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final int maxRetries;

    public OperationLockService(ServicesProperties servicesProperties) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(servicesProperties.operationLock().cache().ttl())
                .maximumSize(servicesProperties.operationLock().cache().maxSize())
                .build();
        this.maxRetries = servicesProperties.operationLock().maxRetries();
    }

    public PaymentResult execute(@NonNull IdempotencyKey idempotencyKey, @NonNull Supplier<PaymentResult> supplier) {
        return getCachedResult(idempotencyKey)
                .filter(entry -> entry.paymentResult().success() || entry.retryCount() >= maxRetries)
                .map(CacheEntry::paymentResult)
                .orElseGet(() -> processNewAction(idempotencyKey, supplier));
    }

    private Optional<CacheEntry> getCachedResult(IdempotencyKey idempotencyKey) {
        var cachedResult = cache.getIfPresent(idempotencyKey.key());
        if (cachedResult == null) {
            return Optional.empty();
        }
        if (!cachedResult.requestBodyHash().equals(idempotencyKey.requestBodyHash())) {
            throw IdempotencyKeyDuplicationException.ofDuplicateKey(idempotencyKey.key());
        }
        return Optional.of(cachedResult);
    }

    private PaymentResult processNewAction(IdempotencyKey idempotencyKey, Supplier<PaymentResult> supplier) {
        var lock = getLock(idempotencyKey.key());
        lock.lock();
        try {
            return getCachedResult(idempotencyKey)
                    .filter(entry -> entry.paymentResult().success() || entry.retryCount() >= maxRetries)
                    .map(CacheEntry::paymentResult)
                    .orElseGet(() -> performSupplierAction(idempotencyKey, supplier));
        } finally {
            lock.unlock();
        }
    }

    private PaymentResult performSupplierAction(IdempotencyKey idempotencyKey, Supplier<PaymentResult> supplier) {
       var newRetryCount = getNextRetryCounterValue(idempotencyKey.key());
        try {
            var result = supplier.get();
            cache.put(idempotencyKey.key(),
                    CacheEntry.builder()
                            .paymentResult(Objects.requireNonNull(result, "Supplier must return a non-null PaymentResult"))
                            .retryCount(newRetryCount)
                            .requestBodyHash(idempotencyKey.requestBodyHash())
                            .build());
            return result;
        } catch (Exception exception) {
            cache.put(idempotencyKey.key(),
                    CacheEntry.builder()
                            .paymentResult(new PaymentResult(false, null))
                            .retryCount(newRetryCount)
                            .requestBodyHash(idempotencyKey.requestBodyHash())
                            .build());
            throw exception;
        }
    }

    private int getNextRetryCounterValue(String idempotencyKey) {
        var currentEntry = cache.getIfPresent(idempotencyKey);
        return currentEntry != null ? currentEntry.retryCount() + 1 : 1;
    }

    private ReentrantLock getLock(String idempotencyKey) {
        return locks.computeIfAbsent(idempotencyKey, key -> new ReentrantLock());
    }
}
