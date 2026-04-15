package com.zilch.interview.validator.cardtoken;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CardTokenValidator implements ConstraintValidator<ValidCardToken, String> {

    private static final Pattern CARD_TOKEN_PATTERN = Pattern.compile("^tok_[a-zA-Z0-9]{10,}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        return CARD_TOKEN_PATTERN.matcher(value).matches();
    }
}
