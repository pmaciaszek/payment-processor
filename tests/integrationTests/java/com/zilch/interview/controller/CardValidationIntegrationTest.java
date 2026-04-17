package com.zilch.interview.controller;

import com.zilch.interview.dto.CardPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.PaymentResponseDTO;
import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.enums.TransferStatus;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class CardValidationIntegrationTest extends IntegrationTest {

    private static final String DEVICE_ID = "device-card-validation";

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
    void shouldReturnBadRequestWhenCardIsSuspended() {
        // given
        var requestDTO = createRequest("tok_suspended01", new BigDecimal("50.00"), "GBP");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .extracting(PaymentProcessorErrorResponseDTO::message)
                .isEqualTo("Card has been suspended");

        assertThat(userTransferRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenCardIsInactive() {
        // given
        var requestDTO = createRequest("tok_inactive01", new BigDecimal("50.00"), "GBP");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .extracting(PaymentProcessorErrorResponseDTO::message)
                .isEqualTo("Card has been deactivated");

        assertThat(userTransferRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenAmountExceedsCardLimit() {
        // given
        var requestDTO = createRequest("tok_lowlimit01", new BigDecimal("150.00"), "GBP");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .extracting(PaymentProcessorErrorResponseDTO::message)
                .isEqualTo("Amount exceeds limits");

        assertThat(userTransferRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenCurrencyNotSupportedByCard() {
        // given
        var requestDTO = createRequest("tok_gbponly001", new BigDecimal("50.00"), "USD");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .extracting(PaymentProcessorErrorResponseDTO::message)
                .isEqualTo("Currency is not supported");

        assertThat(userTransferRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenCardLimitsAreNull() {
        // given
        var requestDTO = createRequest("tok_nolimits01", new BigDecimal("50.00"), "GBP");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .extracting(PaymentProcessorErrorResponseDTO::message)
                .isEqualTo("Currency is not supported");
    }

    @Test
    void shouldSucceedWhenAmountEqualsCardLimit() {
        // given
        var requestDTO = createRequest("tok_exactlimit01", new BigDecimal("100.00"), "GBP");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .returns(true, PaymentResponseDTO::success);

        assertThat(userTransferRepository.findAll())
                .singleElement()
                .satisfies(t -> assertThat(t.getStatus()).isEqualTo(TransferStatus.CAPTURED));
    }

    @Test
    void shouldReturnServiceUnavailableWhenCardValidationReturns404() {
        // given
        var requestDTO = createRequest("tok_notfound01", new BigDecimal("50.00"), "GBP");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.SERVICE_UNAVAILABLE, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldProcessPaymentWithEurCurrency() {
        // given
        var requestDTO = createRequest("tok_eurocard01", new BigDecimal("250.00"), "EUR");

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .returns(true, PaymentResponseDTO::success);
    }

    private PaymentRequestDTO createRequest(String cardToken, BigDecimal amount, String currency) {
        return getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .amount(amount)
                .currency(currency)
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, cardToken))
                .build();
    }
}
