package com.zilch.interview.dto;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.zilch.interview.enums.PaymentMethodType;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPaymentMethodDTO.class, name = "CARD"),
        @JsonSubTypes.Type(value = BlikPaymentMethodDTO.class, name = "BLIK")
})
public interface PaymentMethodDTO {

    PaymentMethodType type();
}
