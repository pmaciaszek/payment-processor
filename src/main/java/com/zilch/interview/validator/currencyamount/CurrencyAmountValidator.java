package com.zilch.interview.validator.currencyamount;

import com.zilch.interview.dto.PaymentRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;

public class CurrencyAmountValidator implements ConstraintValidator<ValidCurrencyAmount, PaymentRequestDTO> {

    private static final String UNSUPPORTED_CURRENCY_CODE_MESSAGE = "Unsupported currency code: %s";
    private static final String INVALID_AMOUNT_SCALE_MESSAGE = "Invalid amount scale for currency %s. Max allowed: %s";

    @Override
    public boolean isValid(PaymentRequestDTO requestDTO, ConstraintValidatorContext context) {
        if (requestDTO == null) {
            return true;
        }

        var currency = getCurrencyByCode(requestDTO.currency());
        if (currency == null) {
            addCustomValidationMessage(context, UNSUPPORTED_CURRENCY_CODE_MESSAGE.formatted(requestDTO.currency()));
            return false;
        }

        var allowedFractionDigits = currency.getDefaultFractionDigits();

        if (requestDTO.amount().stripTrailingZeros().scale() >allowedFractionDigits) {
            addCustomValidationMessage(context, INVALID_AMOUNT_SCALE_MESSAGE.formatted(
                    requestDTO.currency(), allowedFractionDigits));
            return false;
        }
        return true;
    }

    private Currency getCurrencyByCode(String currencyCode) {
        try {
            return Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void addCustomValidationMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
