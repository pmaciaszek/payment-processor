package com.zilch.interview.service;

import com.zilch.interview.config.properties.OperationLockServiceCacheProperties;
import com.zilch.interview.config.properties.OperationLockServiceProperties;
import com.zilch.interview.config.properties.ServicesProperties;
import com.zilch.interview.model.IdempotencyKey;
import com.zilch.interview.model.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLockServiceUnitTest {

    private OperationLockService service;

    @Mock
    Supplier<PaymentResult> supplier;

    @BeforeEach
    void setUp() {
        service = new OperationLockService(
                new ServicesProperties(
                        new OperationLockServiceProperties(
                                5,
                                new OperationLockServiceCacheProperties(
                                        100,
                                        Duration.of(5, ChronoUnit.MINUTES)))));
    }

    @Test
    void shouldReturnSuccessResultAfterInitialFailedAttempt() {
        //given
        var key = new IdempotencyKey("retry-key", "hash1");
        var failedTransactionId = "transaction-failed";
        var successfulTransactionId = "transaction-succeeded";
        when(supplier.get()).thenReturn(
                new PaymentResult(false, failedTransactionId),
                new PaymentResult(true, successfulTransactionId));

        // when
        var result1 = service.execute(key, supplier);
        var result2 = service.execute(key, supplier);

        // then
        assertAll(
                () -> assertThat(result1)
                        .returns(false, PaymentResult::success)
                        .returns(failedTransactionId, PaymentResult::transactionId),
                () -> assertThat(result2)
                        .returns(true, PaymentResult::success)
                        .returns(successfulTransactionId, PaymentResult::transactionId));

        verify(supplier, times(2)).get();
        verifyNoMoreInteractions(supplier);
    }

    @Test
    void shouldRetryUpTo5TimesOnFailedPaymentResult() {
        // given
        var idempotencyKey = new IdempotencyKey("retry-key", "hash1");
        var failedTransactionId = "transaction-failed";
        when(supplier.get()).thenReturn(new PaymentResult(false, failedTransactionId));

        // when
        for( int i = 0; i<6; i++) {
            service.execute(idempotencyKey, supplier);
        }
        var lastResult = service.execute(idempotencyKey, supplier);

        assertThat(lastResult)
                .returns(false, PaymentResult::success)
                .returns(failedTransactionId, PaymentResult::transactionId);

        verify(supplier, times(5)).get();
        verifyNoMoreInteractions(supplier);
    }

    @Test
    void shouldReturnCachedSuccessResult() {
        //given
        var key = new IdempotencyKey("success-key", "hash1");
        var transactionId = "transaction-1";
        when(supplier.get()).thenReturn(new PaymentResult(true, transactionId));

        // when
        var result1 = service.execute(key, supplier);
        var result2 = service.execute(key, supplier);
        
        // then
        assertAll(
                () -> assertThat(result1)
                        .returns(true, PaymentResult::success)
                        .returns(transactionId, PaymentResult::transactionId),
                () -> assertThat(result2)
                        .returns(true, PaymentResult::success)
                        .returns(transactionId, PaymentResult::transactionId));

        verify(supplier).get();
        verifyNoMoreInteractions(supplier);
    }

    @Test
    void shouldHandleConcurrentRequestsWithRetryLimit() throws InterruptedException {
        // given
        var key = new IdempotencyKey("concurrent-key", "hash1");
        var counter = new AtomicInteger(0);
        try (var executor = Executors.newFixedThreadPool(10)) {
            Supplier<PaymentResult> realSupplier = () -> {
                var currentCounter = counter.incrementAndGet();
                return new PaymentResult(false, "transaction-failed-" + currentCounter);
            };

            // when
            var tasks = IntStream.range(0, 20)
                    .<Callable<PaymentResult>>mapToObj(index -> () -> service.execute(key, realSupplier))
                    .toList();

            executor.invokeAll(tasks);
        }

        // then
        assertAll(
                () -> assertThat(counter.get())
                        .isEqualTo(5),
                () -> assertThat(service.execute(key, supplier))
                        .returns(false, PaymentResult::success)
                        .returns("transaction-failed-5", PaymentResult::transactionId));
    }

    @Test
    void shouldIsolateCachePerKey() {
        // given
        var key1 = new IdempotencyKey("key1", "hash1");
        var key2 = new IdempotencyKey("key2", "hash2");
        var transactionId1 = "transaction-1";
        var transactionId2Failed = "transaction-2-failed";
        var transactionId2Success = "transaction-2-succeed";
        when(supplier.get()).thenReturn(
                new PaymentResult(true, transactionId1),
                new PaymentResult(false, transactionId2Failed),
                new PaymentResult(true, transactionId2Success));

        // when
        var result1 = service.execute(key1, supplier);
        var result2 = service.execute(key2, supplier);
        var cachedResult1 = service.execute(key1, supplier);
        var retryResult2 = service.execute(key2, supplier);

        // then
        assertAll(
                () -> assertThat(result1)
                        .returns(true, PaymentResult::success)
                        .returns(transactionId1, PaymentResult::transactionId)
                        .isEqualTo(cachedResult1),
                () -> assertThat(result2)
                        .returns(false, PaymentResult::success)
                        .returns(transactionId2Failed, PaymentResult::transactionId),
                () -> assertThat(retryResult2)
                        .returns(true, PaymentResult::success)
                        .returns(transactionId2Success, PaymentResult::transactionId));

        verify(supplier, times(3)).get();
        verifyNoMoreInteractions(supplier);
    }

    @Test
    void shouldIsolateRetryCountPerKey() {
        // given
        var key1 = new IdempotencyKey("key1", "hash1");
        var key2 = new IdempotencyKey("key2", "hash2");
        var counter1 = new AtomicInteger(0);
        var counter2 = new AtomicInteger(0);

        Supplier<PaymentResult> supplier1 = () -> {
            var count = counter1.incrementAndGet();
            return count >= 2
                    ? new PaymentResult(true, "transaction-success-1")
                    : new PaymentResult(false, "transaction-failed-1");
        };

        Supplier<PaymentResult> supplier2 = () -> {
            counter2.incrementAndGet();
            return new PaymentResult(false, "transaction-failed-2");
        };

        // when
        PaymentResult result1 = null;
        PaymentResult result2 = null;
        for(int i = 0; i<6; i++) {
            result1 = service.execute(key1, supplier1);
            result2 = service.execute(key2, supplier2);
        }

        // then
        assertThat(result1)
                .isNotNull()
                .returns(true, PaymentResult::success)
                .returns("transaction-success-1", PaymentResult::transactionId);
        assertThat(result2)
                .isNotNull()
                .returns(false, PaymentResult::success)
                .returns("transaction-failed-2", PaymentResult::transactionId);

        assertAll(
                () -> assertThat(counter1.get()).isEqualTo(2),
                () -> assertThat(counter2.get()).isEqualTo(5));
    }

    @Test
    void shouldPropagateExceptionAndUpdateCache() {
        // given
        var key = new IdempotencyKey("exception-key", "hash1");
        var counter = new AtomicInteger(0);
        Supplier<PaymentResult> failingSupplier = () -> {
            counter.incrementAndGet();
            throw new RuntimeException("Failed");
        };

        // when && then
        for(int i = 0; i<5; i++) {
            assertThatThrownBy(() -> service.execute(key, failingSupplier))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed");
        }

        assertAll(
                () -> assertThat(counter.get()).isEqualTo(5),
                () -> assertThat(service.execute(key, failingSupplier))
                        .returns(false, PaymentResult::success)
                        .returns(null, PaymentResult::transactionId));
    }

    @Test
    void shouldNotBlockDifferentKeysInConcurrentRequests() throws InterruptedException {
        // given
        var counter = new AtomicInteger(0);
        var duration = 0L;
        try (var executor = Executors.newFixedThreadPool(10)) {
            Supplier<PaymentResult> slowSupplier = () -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
                return new PaymentResult(false, "transaction-" + counter.get());
            };

            // when
            var tasks = IntStream.range(0, 10)
                    .<Callable<PaymentResult>>mapToObj(index ->
                            () -> service.execute(new IdempotencyKey("key" + index, "hash" + index), slowSupplier))
                    .toList();

            var startTime = System.currentTimeMillis();
            executor.invokeAll(tasks);
            duration = System.currentTimeMillis() - startTime;
        }

        // then
        assertThat(counter.get()).isEqualTo(10);
        assertThat(duration).isLessThan(400L);
    }

    @Test
    void shouldReturnNullPointerExceptionWhenSupplierReturnsNull() {
        // given
        var key = new IdempotencyKey("null-supplier-key", "hash1");
        Supplier<PaymentResult> nullSupplier = () -> null;

        // when && then
        assertThatThrownBy(() -> service.execute(key, nullSupplier))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Supplier must return a non-null PaymentResult");
    }

    @Test
    void shouldThrowIdempotencyKeyDuplicationExceptionWhenSameKeyWithDifferentHash() {
        // given
        var key = "same-key";
        var hash1 = "hash1";
        var hash2 = "hash2";
        var idempotencyKey1 = new IdempotencyKey(key, hash1);
        var idempotencyKey2 = new IdempotencyKey(key, hash2);

        when(supplier.get()).thenReturn(new PaymentResult(true, "tx1"));

        // when
        service.execute(idempotencyKey1, supplier);

        // then
        assertThatThrownBy(() -> service.execute(idempotencyKey2, supplier))
                .isInstanceOf(com.zilch.interview.exception.IdempotencyKeyDuplicationException.class)
                .hasMessageContaining("Idempotency key: same-key was used with different request");
    }
}
