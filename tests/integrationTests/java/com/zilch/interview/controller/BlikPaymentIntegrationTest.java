package com.zilch.interview.controller;

import com.zilch.interview.dto.BlikPaymentMethodDTO;
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

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class BlikPaymentIntegrationTest extends IntegrationTest {

    private static final String DEVICE_ID = "device-blik-123";

    @Test
    void shouldProcessBlikPaymentSuccessfully() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());

        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .paymentMethod(new BlikPaymentMethodDTO(PaymentMethodType.BLIK, "654321"))
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .isNotNull()
                .returns(true, PaymentResponseDTO::success)
                .extracting(PaymentResponseDTO::transactionId)
                .isNotNull();

        assertThat(userTransferRepository.findAll())
                .singleElement()
                .satisfies(entity -> assertThat(entity)
                        .returns(user.getId(), UserTransferEntity::getUserId)
                        .returns(requestDTO.amount(), UserTransferEntity::getAmount)
                        .returns(requestDTO.merchantId(), UserTransferEntity::getMerchantId)
                        .returns(requestDTO.orderId(), UserTransferEntity::getOrderId)
                        .returns(TransferStatus.CAPTURED, UserTransferEntity::getStatus));
    }

    @Test
    void shouldReturnBadRequestWhenBlikCodeIsReserved() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .build());

        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .paymentMethod(new BlikPaymentMethodDTO(PaymentMethodType.BLIK, "123456"))
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .isNotNull()
                .returns("BLIK code is not active", PaymentProcessorErrorResponseDTO::message);
        assertThat(userTransferRepository.findAll())
                .isEmpty();
    }
}
