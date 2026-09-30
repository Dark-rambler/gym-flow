package com.gymflow.shared.application.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Contraseña nueva: 8+ caracteres y como máximo 72 bytes UTF-8 (límite de BCrypt;
 * con caracteres multibyte 72 caracteres pueden superar los 72 bytes).
 */
@Documented
@Constraint(validatedBy = ValidPassword.Validator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    int MIN_CHARS = 8;
    int MAX_BYTES = 72;

    String message() default "La contraseña debe tener al menos 8 caracteres (máximo 72 bytes)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidPassword, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value != null
                    && value.length() >= MIN_CHARS
                    && value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
        }
    }
}
