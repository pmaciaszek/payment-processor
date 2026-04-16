package com.zilch.interview.dto.card;

import lombok.NonNull;

public record CardValidationRequestDTO(@NonNull String cardToken, @NonNull String currency) {
}
