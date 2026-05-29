package com.tcs.Machcare.exception;

import com.tcs.Machcare.exception.MachCareException;

public class InventoryException extends MachCareException {

    public InventoryException(String message) {
        super(message);
    }

    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}