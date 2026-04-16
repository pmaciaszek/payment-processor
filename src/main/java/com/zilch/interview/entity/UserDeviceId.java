package com.zilch.interview.entity;

import com.zilch.interview.dto.PaymentRequestDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserDeviceId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    public static UserDeviceId of(PaymentRequestDTO requestDTO) {
        return UserDeviceId.builder()
                .userId(requestDTO.userId())
                .deviceId(requestDTO.deviceId())
                .build();
    }

}
