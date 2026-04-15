package com.zilch.interview.validator.cardtoken;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CardTokenValidator implements ConstraintValidator<ValidCardToken, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        return value.matches("^tok_[a-zA-Z0-9]{10,}$");
    }
}
