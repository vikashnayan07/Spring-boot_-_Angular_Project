package com.tcs.Machcare.exception;

import com.tcs.Machcare.exception.MachCareException;

public class ResourceNotFoundException extends MachCareException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}