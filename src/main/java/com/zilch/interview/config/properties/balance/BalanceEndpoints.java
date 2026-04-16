package com.zilch.interview.config.properties.balance;

import jakarta.validation.constraints.NotBlank;

public record BalanceEndpoints(@NotBlank String userBalance) {
}
