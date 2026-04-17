package com.zilch.interview.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.zilch.interview.dto.BlikPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.PaymentResponseDTO;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.InjectWireMock;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "resilience4j.retry.configs.default.maxAttempts=3",
        "resilience4j.retry.configs.default.waitDuration=100ms",
        "resilience4j.retry.configs.default.enableExponentialBackoff=false",
        "resilience4j.circuitbreaker.configs.default.slidingWindowSize=100",
        "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=100"
})
class RetryIntegrationTest extends IntegrationTest {

    @InjectWireMock
    private WireMockServer wiremock;

    @BeforeEach
    void resetWiremock() {
        wiremock.resetRequests();
        wiremock.resetScenarios();
    }

    @Test
    void shouldRetryAndSucceedAfterTransientFailure() {
        // given - PLN: 2x 500, then 200 (balance_retry.json scenario)
        var request = createRequest("PLN");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, request, PaymentResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.OK, ResponseEntity::getStatusCode);
        wiremock.verify(3, getRequestedFor(urlPathMatching("/v1/balance/[a-f0-9-]{36}"))
                .withQueryParam("currency", equalTo("PLN")));
    }

    @Test
    void shouldFailAfterAllRetriesExhausted() {
        // given
        var request = createRequest("JPY");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, request, String.class);

        // then
        assertThat(response)
                .returns(HttpStatus.SERVICE_UNAVAILABLE, ResponseEntity::getStatusCode);
        wiremock.verify(moreThanOrExactly(2), getRequestedFor(urlPathMatching("/v1/balance/[a-f0-9-]{36}"))
                .withQueryParam("currency", equalTo("JPY")));
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
