package com.tcs.Machcare.exception;

public class MachCareException extends RuntimeException {

    public MachCareException(String message) {
        super(message);
    }

    public MachCareException(String message, Throwable cause) {
        super(message, cause);
    }
}