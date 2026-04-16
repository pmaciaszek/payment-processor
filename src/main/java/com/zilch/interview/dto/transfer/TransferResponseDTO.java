package com.zilch.interview.dto.transfer;

import com.zilch.interview.enums.TransferStatus;

public record TransferResponseDTO(String id, TransferStatus status, String reason) {
}
