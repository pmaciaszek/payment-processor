package com.zilch.interview.validator.currencyamount;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CurrencyAmountValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrencyAmount {

    String message() default "Invalid amount for given currency";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
