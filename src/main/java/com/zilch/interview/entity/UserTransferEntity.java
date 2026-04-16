package com.zilch.interview.entity;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.TransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column
    private String transferId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

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

    public static UserTransferEntity ofPendingTransfer(PaymentRequestDTO requestDTO) {
        return UserTransferEntity.builder()
                .userId(requestDTO.userId())
                .amount(requestDTO.amount())
                .currency(requestDTO.currency())
                .merchantId(requestDTO.merchantId())
                .orderId(requestDTO.orderId())
                .status(TransferStatus.PENDING)
                .build();
    }
}
