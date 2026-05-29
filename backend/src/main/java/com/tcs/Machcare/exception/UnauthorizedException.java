package com.tcs.Machcare.exception;

import com.tcs.Machcare.exception.MachCareException;

public class UnauthorizedException extends MachCareException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}