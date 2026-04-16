package com.zilch.interview.service.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import com.zilch.interview.config.properties.ServicesProperties;
import com.zilch.interview.config.properties.VelocityCheckProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

@ExtendWith(MockitoExtension.class)
class GlobalRequestCounterUnitTest {

    @Mock
    private ServicesProperties servicesProperties;

    private GlobalRequestCounter globalRequestCounter;

    @BeforeEach
    void setUp() {
        when(servicesProperties.velocityCheck()).thenReturn(new VelocityCheckProperties(10, 60L));
        globalRequestCounter = new GlobalRequestCounter(servicesProperties);
    }

    @Test
    void shouldIncrementCounterForDifferentUsers() {
        // given
        var userId1 = UUID.randomUUID();
        var userId2 = UUID.randomUUID();

        // when
        var count1_1 = globalRequestCounter.increment(userId1);
        var count1_2 = globalRequestCounter.increment(userId1);
        var count2_1 = globalRequestCounter.increment(userId2);

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
        globalRequestCounter.increment(userId1);
        globalRequestCounter.increment(userId1);
        globalRequestCounter.increment(userId2);

        // then
        assertAll(
                () -> assertThat(globalRequestCounter.increment(userId1)).isEqualTo(3),
                () -> assertThat(globalRequestCounter.increment(userId2)).isEqualTo(2)
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
                            globalRequestCounter.increment(userId);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then
        assertThat(globalRequestCounter.increment(userId))
                .isEqualTo(numberOfThreads * incrementsPerThread + 1);
    }
}
