package com.zilch.interview.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zilch.interview.config.properties.OperationLockServiceProperties;
import com.zilch.interview.config.properties.ServicesProperties;
import com.zilch.interview.model.PaymentResult;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class OperationLockService {

    private final Cache<String, PaymentResult> cache;
    private final Cache<String, ReentrantLock> locks;
    private final Cache<String, Integer> retryCounts;
    private final OperationLockServiceProperties serviceProperties;

    public OperationLockService(ServicesProperties servicesProperties) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(servicesProperties.operationLock().cache().ttl())
                .maximumSize(servicesProperties.operationLock().cache().maxSize())
                .build();
        this.locks = Caffeine.newBuilder()
                .expireAfterWrite(servicesProperties.operationLock().cache().ttl())
                .maximumSize(servicesProperties.operationLock().cache().maxSize())
                .build();
        this.retryCounts = Caffeine.newBuilder()
                .expireAfterWrite(servicesProperties.operationLock().cache().ttl())
                .maximumSize(servicesProperties.operationLock().cache().maxSize())
                .build();
        this.serviceProperties = servicesProperties.operationLock();
    }

    public PaymentResult execute(@NonNull String idempotencyKey, @NonNull Supplier<PaymentResult> supplier) {
        return getCachedResult(idempotencyKey)
                .filter(cached -> cached.success() || isRetryLimitReached(idempotencyKey))
                .orElseGet(() -> processNewAction(idempotencyKey, supplier));
    }

    private Optional<PaymentResult> getCachedResult(String idempotencyKey) {
        return Optional.ofNullable(cache.getIfPresent(idempotencyKey));
    }

    private boolean isRetryLimitReached(String idempotencyKey) {
        var count = retryCounts.getIfPresent(idempotencyKey);
        return count != null && count >= serviceProperties.maxRetries();
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
        retryCounts.asMap().merge(idempotencyKey, 1, Integer::sum);
        try {
            var result = supplier.get();
            cache.put(idempotencyKey, Objects.requireNonNull(result, "Supplier must return a non-null PaymentResult"));
            return result;
        } catch (Exception exception) {
            cache.put(idempotencyKey, new PaymentResult(false, null));
            throw exception;
        }
    }

    private ReentrantLock getLock(String idempotencyKey) {
        return locks.get(idempotencyKey, key -> new ReentrantLock());
    }
}
