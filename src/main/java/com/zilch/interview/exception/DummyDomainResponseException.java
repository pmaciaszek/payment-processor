package com.zilch.interview.exception;

import com.zilch.interview.dto.DummyDomainErrorResponseDTO;

import java.io.Serial;

public final class DummyDomainResponseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -7348477297061817673L;

    public DummyDomainResponseException(DummyDomainErrorResponseDTO errorResponseDTO) {
        super("%s : %s".formatted(errorResponseDTO.status(), errorResponseDTO.message()));
    }
}
