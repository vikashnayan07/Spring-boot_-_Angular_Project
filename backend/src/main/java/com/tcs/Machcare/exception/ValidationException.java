package com.tcs.Machcare.exception;

import com.tcs.Machcare.exception.MachCareException;

public class ValidationException extends MachCareException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}