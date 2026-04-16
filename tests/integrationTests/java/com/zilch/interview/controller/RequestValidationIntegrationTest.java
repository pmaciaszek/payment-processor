package com.zilch.interview.controller;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import com.zilch.interview.dto.BlikPaymentMethodDTO;
import com.zilch.interview.dto.CardPaymentMethodDTO;
import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

class RequestValidationIntegrationTest extends IntegrationTest {

    private static final String DEVICE_ID = "device-validation";

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
    void shouldReturnBadRequestWhenUserIdIsMissing() {
        // given
        var json = """
                {
                    "amount": 50.00,
                    "currency": "GBP",
                    "merchantId": "merchant-123",
                    "orderId": "order-456",
                    "deviceId": "device-123",
                    "paymentMethod": {"type": "CARD", "cardToken": "tok_1234567890"}
                }
                """;

        // when
        var response = restTestClient.postRawJson(PAYMENTS_ENDPOINT, json, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsMissing() {
        // given
        var json = """
                {
                    "userId": "%s",
                    "currency": "GBP",
                    "merchantId": "merchant-123",
                    "orderId": "order-456",
                    "deviceId": "%s",
                    "paymentMethod": {"type": "CARD", "cardToken": "tok_1234567890"}
                }
                """.formatted(user.getId(), DEVICE_ID);

        // when
        var response = restTestClient.postRawJson(PAYMENTS_ENDPOINT, json, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsNegative() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .amount(new BigDecimal("-10.00"))
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .extracting(PaymentProcessorErrorResponseDTO::message)
                .asString()
                .containsIgnoringCase("amount");
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsZero() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .amount(BigDecimal.ZERO)
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "US", "GBPP", "gb", "123"})
    void shouldReturnBadRequestWhenCurrencyFormatIsInvalid(String invalidCurrency) {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .currency(invalidCurrency)
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnBadRequestWhenCurrencyCodeIsUnknown() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .currency("XXX")
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .extracting(PaymentProcessorErrorResponseDTO::message)
                .asString()
                .containsIgnoringCase("currency");
    }

    @Test
    void shouldReturnBadRequestWhenAmountScaleExceedsCurrencyPrecision() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .amount(new BigDecimal("10.999"))
                .currency("GBP")
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                .extracting(ResponseEntity::getBody)
                .extracting(PaymentProcessorErrorResponseDTO::message)
                .asString()
                .containsIgnoringCase("scale");
    }

    @Test
    void shouldReturnBadRequestWhenJPYHasDecimalPlaces() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .amount(new BigDecimal("100.50"))
                .currency("JPY")
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "tok_", "tok_12345", "1234567890", "TOK_1234567890"})
    void shouldReturnBadRequestWhenCardTokenFormatIsInvalid(String invalidToken) {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, invalidToken))
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "12345a", "abcdef", "12 345"})
    void shouldReturnBadRequestWhenBlikCodeFormatIsInvalid(String invalidBlikCode) {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .paymentMethod(new BlikPaymentMethodDTO(PaymentMethodType.BLIK, invalidBlikCode))
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnBadRequestWhenMerchantIdIsBlank() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .merchantId("   ")
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnBadRequestWhenOrderIdIsBlank() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .orderId("")
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnBadRequestWhenDeviceIdIsBlank() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId("  ")
                .build();

        // when
        var response = restTestClient.post(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnBadRequestWhenRequestIdHeaderIsMissing() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .build();

        // when
        var response = restTestClient.postWithoutRequestId(PAYMENTS_ENDPOINT, requestDTO, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnBadRequestWhenPaymentMethodIsMissing() {
        // given
        var json = """
                {
                    "userId": "%s",
                    "amount": 50.00,
                    "currency": "GBP",
                    "merchantId": "merchant-123",
                    "orderId": "order-456",
                    "deviceId": "%s"
                }
                """.formatted(user.getId(), DEVICE_ID);

        // when
        var response = restTestClient.postRawJson(PAYMENTS_ENDPOINT, json, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }

    @Test
    void shouldReturnBadRequestWhenPaymentMethodTypeIsMissing() {
        // given
        var json = """
                {
                    "userId": "%s",
                    "amount": 50.00,
                    "currency": "GBP",
                    "merchantId": "merchant-123",
                    "orderId": "order-456",
                    "deviceId": "%s",
                    "paymentMethod": {"cardToken": "tok_1234567890"}
                }
                """.formatted(user.getId(), DEVICE_ID);

        // when
        var response = restTestClient.postRawJson(PAYMENTS_ENDPOINT, json, PaymentProcessorErrorResponseDTO.class);

        // then
        assertThat(response)
                .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode);
    }
}
