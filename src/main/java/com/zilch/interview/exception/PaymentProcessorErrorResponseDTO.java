package com.zilch.interview.exception;

import lombok.NonNull;
import org.springframework.http.HttpStatus;

public record PaymentProcessorErrorResponseDTO(@NonNull HttpStatus status,
                                               @NonNull String message) {
}
