package com.zilch.interview.service;

import com.zilch.interview.config.properties.OperationLockServiceProperties;
import com.zilch.interview.model.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
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
        service = new OperationLockService( new OperationLockServiceProperties(5));
    }

    @Test
    void shouldReturnSuccessResultAfterInitialFailedAttempt() {
        //given
        var key = "retry-key";
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
    void shouldRetryUpTo5TimesOnFailedPaymentResult() throws InterruptedException {
        // given
        var idempotencyKey = "retry-key";
        AtomicInteger counter = new AtomicInteger(0);
        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Callable<PaymentResult>> tasks = new ArrayList<>();

        // Symulujemy wiele równoległych żądań
        for (int i = 0; i < numberOfThreads; i++) {
            tasks.add(() -> service.execute(idempotencyKey, () -> {
                try {
                    // Małe opóźnienie, aby wymusić rywalizację i sprawdzić działanie blokad
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
                return new PaymentResult(false, "tx-failed-" + counter.get());
            }));
        }

        // when
        List<Future<PaymentResult>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        for (Future<PaymentResult> future : futures) {
            try {
                assertThat(future.get().success()).isFalse();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        // Powinno być dokładnie 5 wywołań (1 początkowe + 4 retry = 5 prób łącznie)
        assertThat(counter.get()).as("Powinno wywołać supplier dokładnie 5 razy łącznie").isEqualTo(5);

        // 6-ta próba (po zakończeniu wątków) nie powinna już wywoływać supplier'a
        PaymentResult result6 = service.execute(idempotencyKey, () -> {
            counter.incrementAndGet();
            return new PaymentResult(true, "tx-success");
        });
        assertThat(result6.success()).as("Powinno zwrócić scache'owany błąd po przekroczeniu limitu").isFalse();
        assertThat(counter.get()).as("Nie powinno wywołać supplier'a po raz 6-ty").isEqualTo(5);
    }

    @Test
    void shouldReturnCachedSuccessResult() {
        //given
        var key = "success-key";
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
}
