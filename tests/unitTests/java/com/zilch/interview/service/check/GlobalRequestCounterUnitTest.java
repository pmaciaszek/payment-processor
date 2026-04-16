package com.zilch.interview.service.check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class GlobalRequestCounterUnitTest {

    @BeforeEach
    void setUp() {
        GlobalRequestCounter.reset();
    }

    @Test
    void shouldIncrementCounterForDifferentUsers() {
        // given
        var userId1 = UUID.randomUUID();
        var userId2 = UUID.randomUUID();

        // when
        var count1_1 = GlobalRequestCounter.increment(userId1);
        var count1_2 = GlobalRequestCounter.increment(userId1);
        var count2_1 = GlobalRequestCounter.increment(userId2);

        // then
        assertAll(
                () -> assertThat(count1_1).isEqualTo(1),
                () -> assertThat(count1_2).isEqualTo(2),
                () -> assertThat(count2_1).isEqualTo(1)
        );
    }

    @Test
    void shouldMaintainSeparateCounters() {
        // given
        var userId1 = UUID.randomUUID();
        var userId2 = UUID.randomUUID();

        // when
        GlobalRequestCounter.increment(userId1);
        GlobalRequestCounter.increment(userId1);
        GlobalRequestCounter.increment(userId2);

        // then
        assertAll(
                () -> assertThat(GlobalRequestCounter.increment(userId1)).isEqualTo(3),
                () -> assertThat(GlobalRequestCounter.increment(userId2)).isEqualTo(2)
        );
    }

    @Test
    void shouldHandleMultipleThreads() throws InterruptedException {
        // given
        var userId = UUID.randomUUID();
        int numberOfThreads = 10;
        int incrementsPerThread = 100;
        try (var executorService = Executors.newFixedThreadPool(numberOfThreads)) {
            var latch = new CountDownLatch(numberOfThreads);

            // when
            for (int i = 0; i < numberOfThreads; i++) {
                executorService.submit(() -> {
                    try {
                        for (int j = 0; j < incrementsPerThread; j++) {
                            GlobalRequestCounter.increment(userId);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then
        assertThat(GlobalRequestCounter.increment(userId))
                .isEqualTo(numberOfThreads * incrementsPerThread + 1);
    }
}
