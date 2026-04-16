package com.zilch.interview.entity;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.transfer.TransferResponseDTO;
import com.zilch.interview.enums.TransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_transfers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserTransferEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column
    private String statusDescription;

    @CreationTimestamp
    @Column( nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserTransferEntity fromTransferResult(TransferResponseDTO transferResult, PaymentRequestDTO requestDTO) {
        return UserTransferEntity.builder()
                .id(transferResult.id())
                .userId(requestDTO.userId())
                .amount(requestDTO.amount())
                .merchantId(requestDTO.merchantId())
                .orderId(requestDTO.orderId())
                .status(transferResult.status())
                .statusDescription(transferResult.reason())
                .build();
    }
}
