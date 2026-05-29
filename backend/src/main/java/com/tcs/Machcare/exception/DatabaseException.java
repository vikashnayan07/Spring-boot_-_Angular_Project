package com.tcs.Machcare.exception;

import com.tcs.Machcare.exception.MachCareException;

public class DatabaseException extends MachCareException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}