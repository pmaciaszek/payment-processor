package com.zilch.interview.dto.card;

import java.math.BigDecimal;

public record CardLimitsResponseDTO(BigDecimal maxAmount, String currency) {
}
