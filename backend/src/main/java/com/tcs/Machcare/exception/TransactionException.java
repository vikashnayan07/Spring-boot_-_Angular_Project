package com.tcs.Machcare.exception;

import com.tcs.Machcare.exception.MachCareException;

public class TransactionException extends MachCareException {

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}