package com.tcs.Machcare.exception;

import java.util.Collections;
import java.util.List;

public class CsvValidationException extends RuntimeException {

    private final List<String> missingHeaders;
    private final List<String> errors;

    public CsvValidationException(String message, List<String> missingHeaders, List<String> errors) {
        super(message);
        this.missingHeaders = missingHeaders == null ? Collections.emptyList() : missingHeaders;
        this.errors = errors == null ? Collections.emptyList() : errors;
    }

    public List<String> getMissingHeaders() {
        return missingHeaders;
    }

    public List<String> getErrors() {
        return errors;
    }
}
