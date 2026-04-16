package com.zilch.interview.controller;

import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class VelocityCheckIntegrationTest extends IntegrationTest {

    private static final String DEVICE_ID = "device-123";

    @BeforeEach
    void setUp() {
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnBadRequestWhenVelocityLimitExceeded() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.ACTIVE)
                .build());
        userDeviceRepository.save(UserDeviceEntity.builder()
                .id(new UserDeviceId(user.getId(), DEVICE_ID))
                .trusted(true)
                .lastUsedAt(LocalDateTime.now())
                .build());

        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .build();

        // when & then
        int maxRequests = 5;

        for (int i = 0; i < maxRequests; i++) {
            var req = requestDTO.toBuilder().orderId("order-velocity-" + i).build();
            var response = restTestClient.post("/v1/payments", req.orderId(), req, PaymentProcessorErrorResponseDTO.class);
            assertThat(response.getStatusCode())
                    .as("Expected request %d to succeed, but got %s", i, response.getBody())
                    .isEqualTo(HttpStatus.OK);
        }

        var excessRequest = requestDTO.toBuilder().orderId("order-velocity-excess").build();
        var errorResponse = restTestClient.post("/v1/payments", excessRequest.orderId(), excessRequest, PaymentProcessorErrorResponseDTO.class);

        assertThat(errorResponse)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .isNotNull()
                .returns("Too many requests", PaymentProcessorErrorResponseDTO::message);
    }
}
