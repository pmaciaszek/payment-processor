package com.zilch.interview.exception.handler;

import com.zilch.interview.exception.IdempotencyKeyDuplicationException;
import com.zilch.interview.exception.PaymentProcessorErrorResponseDTO;
import com.zilch.interview.exception.ValidationCheckException;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationCheckException.class)
    public ResponseEntity<PaymentProcessorErrorResponseDTO> handleValidationCheckException(ValidationCheckException exception) {
        return logExceptionAndGetResponseDTO(HttpStatus.BAD_REQUEST, exception);
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
                .body(new PaymentProcessorErrorResponseDTO(responseCode, exception.getMessage()));
    }

    private void logException(Exception exception) {
        log.error("An error occurred: %s".formatted(exception.getMessage()),
                StructuredArguments.kv("exceptionName", exception.getClass().getSimpleName()),
                exception);
    }
}
