package com.zilch.interview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CardStatus {
    ACTIVE(true, null),
    EXPIRED(false, "Card has expired"),
    SUSPENDED(false, "Card has been suspended"),
    INACTIVE(false, "Card has been deactivated");

    final boolean isValid;
    final String validationMessage;
}
