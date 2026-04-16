package com.zilch.interview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransferStatus {
    CAPTURED(true),
    FAILED(false);

    private final boolean success;
}
