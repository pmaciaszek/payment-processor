package com.zilch.interview.utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConstraintValidationProvider {

    public static <T> Set<ConstraintViolation<T>> validate(T value) {
        try (var validatorFactory = buildValidatorFactory()) {
            return validatorFactory.getValidator().validate(value);
        }
    }

    private static ValidatorFactory buildValidatorFactory() {
        return Validation.byDefaultProvider()
                .configure()
                .constraintValidatorFactory(new SpringConstraintValidatorFactory(new DefaultListableBeanFactory()))
                .buildValidatorFactory();
    }
}
