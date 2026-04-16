package com.zilch.interview.dto.card;

import com.zilch.interview.enums.CardStatus;

public record CardValidationResponseDTO(CardStatus status, CardLimitsResponseDTO limits) {
}
