package com.zilch.interview.service.check.paymentmethod;

import com.zilch.interview.client.CardClient;
import com.zilch.interview.dto.CardPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.card.CardValidationRequestDTO;
import com.zilch.interview.dto.card.CardValidationResponseDTO;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.model.CheckResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardPaymentMethodValidator implements PaymentMethodValidator<CardPaymentMethodDTO> {

    private final CardClient cardClient;

    @Override
    public boolean isApplicable(PaymentMethodType type) {
        return PaymentMethodType.CARD == type;
    }

    @Override
    public CheckResult validate(PaymentRequestDTO paymentRequestDTO) {
        var paymentMethod = getPaymentMethod(paymentRequestDTO);
        var validationResult = getValidationResult(paymentMethod.cardToken(), paymentRequestDTO.currency());
        if (validationResult == null) {
            return CheckResult.fail("Invalid card token");
        }
        if (!validationResult.status().isValid()) {
            return CheckResult.fail(validationResult.status().getValidationMessage());
        }
        if (validationResult.limits().maxAmount() == null
                || !validationResult.limits().currency().equals(paymentRequestDTO.currency())) {
            return CheckResult.fail("Currency is not supported");
        }
        if (validationResult.limits().maxAmount().compareTo(paymentRequestDTO.amount()) < 0) {
            return CheckResult.fail("Amount exceeds limits");
        }

        return CheckResult.ok();
    }

    private CardValidationResponseDTO getValidationResult(String cardToken, String currency) {
        return cardClient.validateCard(new CardValidationRequestDTO(cardToken, currency));
    }
}
