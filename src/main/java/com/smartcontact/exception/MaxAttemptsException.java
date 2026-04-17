package com.smartcontact.exception;

public class MaxAttemptsException extends RuntimeException {
    public MaxAttemptsException(String msg) {
        super(msg);
}
}
