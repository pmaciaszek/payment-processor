package com.zilch.interview.exception.handler;

import com.zilch.interview.exception.BalanceResponseException;
import com.zilch.interview.exception.CardResponseException;
import com.zilch.interview.exception.IdempotencyKeyDuplicationException;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.exception.ValidationCheckException;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({BalanceResponseException.class, CardResponseException.class})
    public ResponseEntity<PaymentProcessorErrorResponseDTO> handleIntegrationExceptions(Exception exception) {
        return logExceptionAndGetResponseDTO(HttpStatus.SERVICE_UNAVAILABLE, exception);
    }

    @ExceptionHandler(ValidationCheckException.class)
    public ResponseEntity<PaymentProcessorErrorResponseDTO> handleValidationCheckException(ValidationCheckException exception) {
        return logExceptionAndGetResponseDTO(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<PaymentProcessorErrorResponseDTO> handleCompletionException(CompletionException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof ValidationCheckException validationException) {
            return handleValidationCheckException(validationException);
        }
        return logExceptionAndGetResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        var errors = extractErrors(exception);
        log.error("An error occurred: {}", errors, exception);
        return ResponseEntity
                .status(status)
                .body(new PaymentProcessorErrorResponseDTO(status.toString(), errors));
    }

    private String extractErrors(MethodArgumentNotValidException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .map(error -> "Field '%s' %s".formatted(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
    }

    @ExceptionHandler(IdempotencyKeyDuplicationException.class)
    public ResponseEntity<PaymentProcessorErrorResponseDTO> handleIdempotencyKeyDuplicationException(IdempotencyKeyDuplicationException exception) {
        return logExceptionAndGetResponseDTO(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PaymentProcessorErrorResponseDTO> handleGeneralException(Exception exception) {
        return logExceptionAndGetResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    private ResponseEntity<PaymentProcessorErrorResponseDTO> logExceptionAndGetResponseDTO(HttpStatus responseCode,
                                                                                           Exception exception) {
        logException(exception);
        return ResponseEntity
                .status(responseCode)
                .body(new PaymentProcessorErrorResponseDTO(responseCode.name(), exception.getMessage()));
    }

    private void logException(Exception exception) {
        log.error("An error occurred: %s".formatted(exception.getMessage()),
                StructuredArguments.kv("exceptionName", exception.getClass().getSimpleName()),
                exception);
    }
}
