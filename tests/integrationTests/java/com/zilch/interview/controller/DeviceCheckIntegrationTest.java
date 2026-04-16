package com.zilch.interview.controller;

import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class DeviceCheckIntegrationTest extends IntegrationTest {

    @Test
    void shouldReturnBadRequestWhenDeviceNotRecognized() {
        // Given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());

        var request = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId("unknown-device")
                .build();

        // When
        var response = restTestClient.post(PAYMENTS_ENDPOINT, request, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .isNotNull()
                .returns("Device not recognized", PaymentProcessorErrorResponseDTO::message);
        assertThat(userTransferRepository.findAll())
                .isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenDeviceIsNotTrusted() {
        // Given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());

        var deviceId = "untrusted-device";
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), deviceId))
                .trusted(false)
                .build());

        var request = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(deviceId).build();

        // When
        var response = restTestClient.post(PAYMENTS_ENDPOINT, request, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .isNotNull()
                .returns("Device is not trusted", PaymentProcessorErrorResponseDTO::message);
        assertThat(userTransferRepository.findAll())
                .isEmpty();
    }
}
