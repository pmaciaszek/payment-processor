package com.zilch.interview.controller;

import com.zilch.interview.dto.CardPaymentMethodDTO;
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

import java.util.UUID;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class IdempotencyIntegrationTest extends IntegrationTest {

    private static final String DEVICE_ID = "device-idempotency-123";

    @Test
    void shouldReturnSameResponseForSameIdempotencyKey() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());

        var idempotencyKey = UUID.randomUUID().toString();
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .orderId("order-idempotency-1")
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_1234567890"))
                .build();

        // when
        var firstResponse = restTestClient.post(PAYMENTS_ENDPOINT, idempotencyKey, requestDTO, PaymentResponseDTO.class);
        var secondResponse = restTestClient.post(PAYMENTS_ENDPOINT, idempotencyKey, requestDTO, PaymentResponseDTO.class);

        // then
        assertAll(
                () -> assertThat(firstResponse)
                        .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .isNotNull()
                        .returns(true, PaymentResponseDTO::success)
                        .extracting(PaymentResponseDTO::transactionId)
                        .isNotNull(),
                () -> assertThat(secondResponse)
                        .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .isNotNull()
                        .returns(true, PaymentResponseDTO::success)
                        .returns(firstResponse.getBody().transactionId(), PaymentResponseDTO::transactionId),
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
    void shouldReturnConflictWhenSameIdempotencyKeyUsedWithDifferentBody() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());

        var idempotencyKey = UUID.randomUUID().toString();
        var expectedMessage = "Idempotency key: %s was used with different request".formatted(idempotencyKey);
        var requestDTO1 = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .orderId("order-idempotency-A")
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_1234567890"))
                .build();

        var requestDTO2 = requestDTO1.toBuilder()
                .orderId("order-idempotency-B")
                .build();

        // when
        restTestClient.post(PAYMENTS_ENDPOINT, idempotencyKey, requestDTO1, PaymentResponseDTO.class);
        var secondResponse = restTestClient.post(PAYMENTS_ENDPOINT, idempotencyKey, requestDTO2, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(secondResponse)
                .returns(HttpStatus.CONFLICT, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .isNotNull()
                .returns(expectedMessage, PaymentProcessorErrorResponseDTO::message);
    }
}
