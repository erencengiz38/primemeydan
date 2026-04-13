package com.meydan.meydan.exception;

import org.springframework.http.HttpStatus;

public class PlayerAlreadyBusyException extends BaseException {
    public PlayerAlreadyBusyException(String message) {
        super(ErrorCode.VAL_001, message, HttpStatus.CONFLICT, "Oyuncu, tarihleri çakışan başka bir turnuvada asil kadroda yer alıyor.");
    }
}
