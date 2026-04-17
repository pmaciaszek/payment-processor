package com.zilch.interview.controller;

import com.zilch.interview.dto.BlikPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.utils.base.IntegrationTest;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "resilience4j.bulkhead.configs.default.maxConcurrentCalls=2",
        "resilience4j.bulkhead.configs.default.maxWaitDuration=0ms",
        "resilience4j.retry.configs.default.maxAttempts=1",
        "resilience4j.circuitbreaker.configs.default.slidingWindowSize=100",
        "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=100"
})
class BulkheadIntegrationTest extends IntegrationTest {

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @BeforeEach
    void resetBulkheads() {
        bulkheadRegistry.getAllBulkheads()
                .forEach(bulkhead -> {
                    while (bulkhead.getMetrics().getAvailableConcurrentCalls()
                            < bulkhead.getBulkheadConfig().getMaxConcurrentCalls()) {
                        bulkhead.releasePermission();
                    }
                });
    }

    @Test
    void shouldRejectRequestsWhenBulkheadIsFull() {
        // given - manually acquire all bulkhead permits
        var balanceBulkhead = bulkheadRegistry.bulkhead("balanceService");
        balanceBulkhead.acquirePermission();
        balanceBulkhead.acquirePermission();

        assertThat(balanceBulkhead.getMetrics().getAvailableConcurrentCalls()).isZero();

        var request = createRequest("DKK");

        try {
            // when - request should be rejected because bulkhead is full
            var response = restTestClient.post(PAYMENTS_ENDPOINT, request, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        } finally {
            balanceBulkhead.releasePermission();
            balanceBulkhead.releasePermission();
        }
    }

    @Test
    void shouldAllowRequestsWhenBulkheadHasCapacity() {
        // given
        var request = createRequest("DKK");

        // when
        var response1 = restTestClient.post(PAYMENTS_ENDPOINT, request, String.class);
        var response2 = restTestClient.post(PAYMENTS_ENDPOINT, request, String.class);

        // then
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReportBulkheadMetrics() {
        // given
        var bulkhead = bulkheadRegistry.bulkhead("balanceService");

        // then
        assertThat(bulkhead.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
    }

    private PaymentRequestDTO createRequest(String currency) {
        return getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .currency(currency)
                .paymentMethod(new BlikPaymentMethodDTO(PaymentMethodType.BLIK, "654321"))
                .amount(new BigDecimal("50.00"))
                .build();
    }
}
