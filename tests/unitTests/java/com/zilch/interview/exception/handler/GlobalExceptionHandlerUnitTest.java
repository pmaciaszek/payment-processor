package com.zilch.interview.exception.handler;

import com.zilch.interview.dto.balance.BalanceErrorResponseDTO;
import com.zilch.interview.dto.card.CardErrorResponseDTO;
import com.zilch.interview.exception.BalanceResponseException;
import com.zilch.interview.exception.CardResponseException;
import com.zilch.interview.exception.IdempotencyKeyDuplicationException;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.exception.ValidationCheckException;
import com.zilch.interview.model.CheckResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleValidationCheckException(CapturedOutput output) {
        // given
        var exception = ValidationCheckException.of(CheckResult.fail("validation error"));

        // when
        var response = handler.handleValidationCheckException(exception);

        // then
        assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .returns("validation error", PaymentProcessorErrorResponseDTO::message),
                () -> assertThat(output.getOut()).contains("An error occurred: validation error")
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
        assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.CONFLICT, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .returns(expectedMessage, PaymentProcessorErrorResponseDTO::message),
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
        assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.INTERNAL_SERVER_ERROR, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .returns(message, PaymentProcessorErrorResponseDTO::message),
                () -> assertThat(output.getOut()).contains("An error occurred: " + message)
        );
    }

    @ParameterizedTest
    @MethodSource("providerForShouldHandleIntegrationExceptions")
    void shouldHandleIntegrationExceptions(Exception exception, String errorMessage, CapturedOutput output) {
        // when
        var response = handler.handleIntegrationExceptions(exception);

        // then
        assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.SERVICE_UNAVAILABLE, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .returns(errorMessage, PaymentProcessorErrorResponseDTO::message),
                () -> assertThat(output.getOut()).contains("An error occurred: " + errorMessage));
    }

    private static Stream<Arguments> providerForShouldHandleIntegrationExceptions() {
        return Stream.of(
                Arguments.of(new BalanceResponseException(
                                new BalanceErrorResponseDTO(500, "Balance integration error")),
                        "500 : Balance integration error"),
                Arguments.of(new CardResponseException(
                                new CardErrorResponseDTO("500", "Card integration error")),
                        "500 : Card integration error"));
    }

    @Test
    void shouldHandleCompletionExceptionWithValidationCause(CapturedOutput output) {
        // given
        var validationException = ValidationCheckException.of(CheckResult.fail("validation error"));
        var completionException = new CompletionException(validationException);

        // when
        var response = handler.handleCompletionException(completionException);

        // then
        assertAll(
                () -> assertThat(response)
                        .returns(HttpStatus.BAD_REQUEST, ResponseEntity::getStatusCode)
                        .extracting(ResponseEntity::getBody)
                        .returns("validation error", PaymentProcessorErrorResponseDTO::message),
                () -> assertThat(output.getOut()).contains("An error occurred: validation error")
        );
    }

    @Test
    void shouldHandleCompletionExceptionWithGeneralCause(CapturedOutput output) {
        // given
        var cause = new RuntimeException("Generic error");
        var completionException = new CompletionException(cause);

        // when
        var response = handler.handleCompletionException(completionException);

        // then
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                () -> assertThat(output.getOut()).contains("An error occurred: java.lang.RuntimeException: Generic error")
        );
    }

    @Test
    void shouldHandleMethodArgumentNotValid(CapturedOutput output) {
        // given
        var exception = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("object", "field", "is invalid");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        // when
        var response = handler.handleMethodArgumentNotValid(exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, null);

        // then
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(((PaymentProcessorErrorResponseDTO) response.getBody()).message())
                        .isEqualTo("Field 'field' is invalid"),
                () -> assertThat(output.getOut()).contains("An error occurred: Field 'field' is invalid")
        );
    }
}
