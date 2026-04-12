package com.meydan.meydan.exception;

import org.springframework.http.HttpStatus;

public class InSufficientBalanceException extends BaseException {

    public InSufficientBalanceException(String message) {
        super(ErrorCode.VAL_001, message, HttpStatus.PAYMENT_REQUIRED, "Lütfen bakiye yükleyiniz.");
    }
}
