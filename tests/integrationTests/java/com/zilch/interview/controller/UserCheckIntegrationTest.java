package com.zilch.interview.controller;

import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class UserCheckIntegrationTest extends IntegrationTest {

    @BeforeEach
    void setUp() {
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnBadRequestWhenUserNotFound() {
        // given
        var nonExistentUserId = UUID.randomUUID();
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(nonExistentUserId)
                .build();

        // when
        var response = restTestClient.post("/v1/payments", UUID.randomUUID().toString(), requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .isNotNull()
                .returns("User not found", PaymentProcessorErrorResponseDTO::message);
    }

    @Test
    void shouldReturnBadRequestWhenUserIsNotActive() {
        // given
        var user = userRepository.save(UserEntity.builder()
                .status(UserAccountStatus.INACTIVE)
                .build());
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .build();

        // when
        var response = restTestClient.post("/v1/payments", UUID.randomUUID().toString(), requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .isNotNull()
                .returns("User is not active", PaymentProcessorErrorResponseDTO::message);
    }
}
