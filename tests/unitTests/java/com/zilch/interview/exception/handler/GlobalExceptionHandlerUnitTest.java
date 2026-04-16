package com.zilch.interview.exception.handler;

import com.zilch.interview.exception.IdempotencyKeyDuplicationException;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.exception.ValidationCheckException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleValidationCheckException(CapturedOutput output) {
        // given
        var message = "Validation failed";
        var exception = ValidationCheckException.empty();

        // when
        var response = handler.handleValidationCheckException(exception);

        // then
        Assertions.assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .extracting(PaymentProcessorErrorResponseDTO::message)
                        .isEqualTo("There were some validation errors"),
                () -> assertThat(output.getOut()).contains("An error occurred: There were some validation errors")
        );
    }

    @Test
    void shouldHandleIdempotencyKeyDuplicationException(CapturedOutput output) {
        // given
        var key = "test-key";
        var exception = IdempotencyKeyDuplicationException.ofDuplicateKey(key);
        var expectedMessage = "Idempotency key: test-key was used with different request";

        // when
        var response = handler.handleIdempotencyKeyDuplicationException(exception);

        // then
        Assertions.assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.CONFLICT, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .extracting(PaymentProcessorErrorResponseDTO::message)
                        .isEqualTo(expectedMessage),
                () -> assertThat(output.getOut()).contains("An error occurred: " + expectedMessage)
        );
    }

    @Test
    void shouldHandleGeneralException(CapturedOutput output) {
        // given
        var message = "Unexpected error";
        var exception = new RuntimeException(message);

        // when
        var response = handler.handleGeneralException(exception);

        // then
        Assertions.assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.INTERNAL_SERVER_ERROR, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .extracting(PaymentProcessorErrorResponseDTO::message)
                        .isEqualTo(message),
                () -> assertThat(output.getOut()).contains("An error occurred: " + message)
        );
    }
}
