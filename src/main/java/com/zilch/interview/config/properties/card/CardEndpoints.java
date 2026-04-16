package com.zilch.interview.config.properties.card;

import jakarta.validation.constraints.NotBlank;

public record CardEndpoints(@NotBlank String cardValidation) {
}
