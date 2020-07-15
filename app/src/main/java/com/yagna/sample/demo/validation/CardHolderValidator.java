package com.yagna.sample.demo.validation;

import android.text.TextUtils;

import com.yagna.sample.demo.R;

import static com.yagna.sample.demo.validation.FieldValidationResult.fail;
import static com.yagna.sample.demo.validation.FieldValidationResult.valid;

public class CardHolderValidator implements FieldValidator<CharSequence> {
    @Override
    public FieldValidationResult validate(CharSequence value) {
        if (TextUtils.isEmpty(value)) {
            return fail(R.string.validation_error_fill_in_card_holder_name);
        }
        if (!value.toString().trim().contains(" ")) {
            return fail(R.string.validation_error_invalid_card_holder_name);
        }
        return valid();
    }
}
