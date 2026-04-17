package com.zilch.interview.controller;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.utils.base.IntegrationTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.configs.default.slidingWindowSize=5",
        "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=3",
        "resilience4j.circuitbreaker.configs.default.failureRateThreshold=50",
        "resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=10s",
        "resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState=2",
        "resilience4j.circuitbreaker.configs.default.automaticTransitionFromOpenToHalfOpenEnabled=false",
        "resilience4j.retry.configs.default.maxAttempts=1"
})
class CircuitBreakerIntegrationTest extends IntegrationTest {

    private static final String DEVICE_ID = "device-cb-test";
    private static final String CB_BALANCE_SERVICE = "balanceService";

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private UserEntity user;

    @BeforeEach
    void setUpUser() {
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(CircuitBreaker::reset);

        user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());
    }

    @Test
    void shouldOpenCircuitBreakerAfterMultipleBalanceServiceFailures() {
        // given
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker(CB_BALANCE_SERVICE);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        var failingRequest = createBalanceRequest("JPY");

        // when
        for (int i = 0; i < 3; i++) {
            restTestClient.post(PAYMENTS_ENDPOINT, failingRequest, PaymentProcessorErrorResponseDTO.class);
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isGreaterThanOrEqualTo(50.0f);
    }

    @Test
    void shouldRejectRequestsImmediatelyWhenCircuitBreakerIsOpen() {
        // given
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker(CB_BALANCE_SERVICE);
        var failingRequest = createBalanceRequest("JPY");

        for (int i = 0; i < 3; i++) {
            restTestClient.post(PAYMENTS_ENDPOINT, failingRequest, PaymentProcessorErrorResponseDTO.class);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        long notPermittedBefore = circuitBreaker.getMetrics().getNumberOfNotPermittedCalls();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, failingRequest, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.SERVICE_UNAVAILABLE, ResponseEntity::getStatusCode);
        assertThat(response.getBody())
                .extracting(PaymentProcessorErrorResponseDTO::message)
                .asString()
                .containsIgnoringCase("unavailable");

        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls())
                .isGreaterThan(notPermittedBefore);
    }

    @Test
    void shouldCloseCircuitBreakerAfterSuccessfulCallsInHalfOpen() {
        // given
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker(CB_BALANCE_SERVICE);
        var successRequest = createBalanceRequest("GBP");

        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();

        // when
        for (int i = 0; i < 2; i++) {
            restTestClient.post(PAYMENTS_ENDPOINT, successRequest, String.class);
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void shouldReopenCircuitBreakerIfHalfOpenCallsFail() {
        // given
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker(CB_BALANCE_SERVICE);
        var failingRequest = createBalanceRequest("JPY");

        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();

        // when
        restTestClient.post(PAYMENTS_ENDPOINT, failingRequest, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldNotCountBusinessErrorsAsCircuitBreakerFailures() {
        // given
        var circuitBreaker = circuitBreakerRegistry.circuitBreaker(CB_BALANCE_SERVICE);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        var businessErrorRequest = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .currency("GBP")
                .amount(new BigDecimal("999.00"))
                .build();

        // when
        for (int i = 0; i < 5; i++) {
            restTestClient.post(PAYMENTS_ENDPOINT, businessErrorRequest, PaymentProcessorErrorResponseDTO.class);
        }

        // then
        assertThat(circuitBreaker)
                .returns(CircuitBreaker.State.CLOSED, CircuitBreaker::getState)
                .extracting(CircuitBreaker::getMetrics)
                .returns(0, CircuitBreaker.Metrics::getNumberOfFailedCalls);
    }


    private PaymentRequestDTO createBalanceRequest(String currency) {
        return getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .currency(currency)
                .amount(new BigDecimal("50.00"))
                .build();
    }
}
