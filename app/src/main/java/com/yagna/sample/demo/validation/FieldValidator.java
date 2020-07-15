package com.yagna.sample.demo.validation;

public interface FieldValidator<V> {
    FieldValidationResult validate(V value);
}
