package com.zilch.interview.controller;

import com.zilch.interview.dto.CardPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.PaymentResponseDTO;
import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.entity.UserTransferEntity;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.enums.TransferStatus;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PaymentsControllerIntegrationTest extends IntegrationTest {

    private static final String DEVICE_ID = "device-123";

    @Test
    void shouldProcessCardPaymentSuccessfully() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());
        var cardToken = "tok_1234567890";
        var amount = new BigDecimal("50.00");
        var currency = "GBP";

        var requestDTO = PaymentRequestDTO.builder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .merchantId("merchant-123")
                .orderId("order-456")
                .amount(amount)
                .currency(currency)
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, cardToken))
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentResponseDTO.class);

        // then
        assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .returns(true, PaymentResponseDTO::success)
                        .extracting(PaymentResponseDTO::transactionId)
                        .isNotNull(),
                () -> assertThat(userTransferRepository.findAll())
                        .singleElement()
                        .satisfies(entity -> assertThat(entity)
                                .returns(user.getId(), UserTransferEntity::getUserId)
                                .returns(requestDTO.amount(), UserTransferEntity::getAmount)
                                .returns(requestDTO.merchantId(), UserTransferEntity::getMerchantId)
                                .returns(requestDTO.orderId(), UserTransferEntity::getOrderId)
                                .returns(TransferStatus.CAPTURED, UserTransferEntity::getStatus)));
    }

    @Test
    void shouldReturnBadRequestWhenBalanceIsInsufficient() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());

        var requestDTO = PaymentRequestDTO.builder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .merchantId("merchant-123")
                .orderId("order-456")
                .amount(new BigDecimal("202.00"))
                .currency("GBP")
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_1234567890"))
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .returns("Insufficient funds", PaymentProcessorErrorResponseDTO::message);
    }

    @Test
    void shouldReturnBadRequestWhenCardIsExpired() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());

        var requestDTO = PaymentRequestDTO.builder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .merchantId("merchant-123")
                .orderId("order-456")
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_expired001"))
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .returns("Card has expired", PaymentProcessorErrorResponseDTO::message);
    }

    @Test
    void shouldReturnBadRequestWhenVelocityCheckFails() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());

        var requestDTO = PaymentRequestDTO.builder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .merchantId("merchant-123")
                .orderId("order-velocity")
                .amount(new BigDecimal("10.00"))
                .currency("GBP")
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_1234567890"))
                .build();

        for (int i = 0; i < 5; i++) {
            restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, String.class);
        }
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .returns("Too many requests", PaymentProcessorErrorResponseDTO::message);
    }

    @Test
    void shouldReturnUnsuccessfulResultWhenTransferFails() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());

        var requestDTO = PaymentRequestDTO.builder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .merchantId("merchant-123")
                .orderId("order-fail-transfer")
                .amount(new BigDecimal("9.99"))
                .currency("GBP")
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_1234567890"))
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentResponseDTO.class);

        // then
        assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .returns(false, PaymentResponseDTO::success)
                        .extracting(PaymentResponseDTO::transactionId)
                        .isNotNull(),
                () -> assertThat(userTransferRepository.findAll())
                        .singleElement()
                        .satisfies(entity -> assertThat(entity)
                                .returns(user.getId(), UserTransferEntity::getUserId)
                                .returns(requestDTO.amount(), UserTransferEntity::getAmount)
                                .returns(requestDTO.merchantId(), UserTransferEntity::getMerchantId)
                                .returns(requestDTO.orderId(), UserTransferEntity::getOrderId)
                                .returns(TransferStatus.FAILED, UserTransferEntity::getStatus)));
    }
}
