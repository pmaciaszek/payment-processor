package com.zilch.interview.exception;

import com.zilch.interview.dto.balance.BalanceErrorResponseDTO;

import java.io.Serial;

public final class BalanceResponseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -7348477297061817673L;

    public BalanceResponseException(BalanceErrorResponseDTO errorResponseDTO) {
        super("%s : %s".formatted(errorResponseDTO.status(), errorResponseDTO.message()));
    }
}
