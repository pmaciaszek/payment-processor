package com.zilch.interview.controller;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import com.zilch.interview.dto.CardPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

class ExternalServiceFailureIntegrationTest extends IntegrationTest {

    private static final String DEVICE_ID = "device-ext-failure";

    private UserEntity user;

    @BeforeEach
    void setUpUser() {
        user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());
    }

    @Test
    void shouldReturnInternalServerErrorWhenBalanceServiceReturns503() {
        // given
        var requestDTO = createBalanceRequest("CHF");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.INTERNAL_SERVER_ERROR, ResponseEntity::getStatusCode);
        assertThat(userTransferRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnInternalServerErrorWhenCardServiceReturns503() {
        // given
        var requestDTO = createCardRequest("tok_error503001");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.INTERNAL_SERVER_ERROR, ResponseEntity::getStatusCode);
        assertThat(userTransferRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnInternalServerErrorWhenBalanceServiceReturns500() {
        // given
        var requestDTO = createBalanceRequest("JPY");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.INTERNAL_SERVER_ERROR, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnInternalServerErrorWhenCardServiceReturns500() {
        // given
        var requestDTO = createCardRequest("tok_error500001");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.INTERNAL_SERVER_ERROR, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldTimeoutWhenBalanceServiceIsSlow() {
        // given
        var requestDTO = createBalanceRequest("AUD");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .satisfies(r -> assertThat(r.getStatusCode())
                        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void shouldTimeoutWhenCardServiceIsSlow() {
        // given
        var requestDTO = createCardRequest("tok_slow010000");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .satisfies(r -> assertThat(r.getStatusCode())
                        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void shouldNotCreateTransferWhenValidationFails() {
        // given
        var requestDTO = createBalanceRequest("JPY");

        // when
        restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(userTransferRepository.findAll()).isEmpty();
    }

    @Test
    void shouldHandleBalanceServiceReturningInvalidJson() {
        // given
        var requestDTO = createBalanceRequest("CAD");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.INTERNAL_SERVER_ERROR, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldHandleCardServiceReturningInvalidJson() {
        // given
        var requestDTO = createCardRequest("tok_badjson001");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.INTERNAL_SERVER_ERROR, ResponseEntity::getStatusCode);
    }

    private PaymentRequestDTO createCardRequest(String cardToken) {
        return getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, cardToken))
                .build();
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
