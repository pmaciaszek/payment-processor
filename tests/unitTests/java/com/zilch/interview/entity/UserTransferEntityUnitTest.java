package com.zilch.interview.entity;

import com.zilch.interview.dto.transfer.TransferResponseDTO;
import com.zilch.interview.enums.TransferStatus;
import com.zilch.interview.utils.PaymentRequestDTOProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTransferEntityUnitTest {

    @Test
    void shouldMapFromTransferResult() {
        // given
        var userId = UUID.randomUUID();
        var requestDTO = PaymentRequestDTOProvider.getPaymentDTORequestBuilder()
                .userId(userId)
                .amount(java.math.BigDecimal.valueOf(100.50))
                .currency("PLN")
                .merchantId("M-1")
                .orderId("O-1")
                .build();

        var transferResult = new TransferResponseDTO("T-1", TransferStatus.CAPTURED, "OK");

        // when
        var entity = UserTransferEntity.fromTransferResult(transferResult, requestDTO);

        // then
        assertThat(entity)
                .returns("T-1", UserTransferEntity::getId)
                .returns(userId, UserTransferEntity::getUserId)
                .returns(BigDecimal.valueOf(100.50), UserTransferEntity::getAmount)
                .returns("M-1", UserTransferEntity::getMerchantId)
                .returns("O-1", UserTransferEntity::getOrderId)
                .returns(TransferStatus.CAPTURED, UserTransferEntity::getStatus)
                .returns("OK", UserTransferEntity::getStatusDescription);
    }
}
