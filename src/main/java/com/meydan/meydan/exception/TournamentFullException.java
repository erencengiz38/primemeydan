package com.meydan.meydan.exception;

public class TournamentFullException extends RuntimeException {
    public TournamentFullException(String message) {
        super(message);
    }
}