package com.zilch.interview.exception;

import com.zilch.interview.dto.card.CardErrorResponseDTO;

import java.io.Serial;

public final class CardResponseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 4954732473734338954L;

    public CardResponseException(CardErrorResponseDTO errorResponseDTO) {
        super("%s : %s".formatted(errorResponseDTO.status(), errorResponseDTO.errorDescription()));
    }
}
