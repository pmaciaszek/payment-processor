package com.zilch.interview.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.zilch.interview.enums.TransferStatus;
import com.zilch.interview.utils.PaymentRequestDTOProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

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

        // when
        var entity = UserTransferEntity.ofPendingTransfer(requestDTO);

        // then
        assertThat(entity)
                .returns(userId, UserTransferEntity::getUserId)
                .returns(BigDecimal.valueOf(100.50), UserTransferEntity::getAmount)
                .returns("M-1", UserTransferEntity::getMerchantId)
                .returns("O-1", UserTransferEntity::getOrderId)
                .returns(TransferStatus.PENDING, UserTransferEntity::getStatus);
    }
}
