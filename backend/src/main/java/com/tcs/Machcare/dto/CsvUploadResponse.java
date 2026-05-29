package com.tcs.Machcare.dto;

import java.util.Collections;
import java.util.List;

public class CsvUploadResponse {

    private boolean success;
    private int imported;
    private int failed;
    private int skipped;
    private String message;
    private List<String> missingHeaders = Collections.emptyList();
    private List<String> errors = Collections.emptyList();

    public static CsvUploadResponse success(int imported, int skipped, String message) {
        CsvUploadResponse response = new CsvUploadResponse();
        response.setSuccess(true);
        response.setImported(imported);
        response.setFailed(0);
        response.setSkipped(skipped);
        response.setMessage(message);
        return response;
    }

    public static CsvUploadResponse invalid(String message, List<String> missingHeaders, List<String> errors) {
        CsvUploadResponse response = new CsvUploadResponse();
        response.setSuccess(false);
        response.setImported(0);
        response.setFailed(0);
        response.setSkipped(0);
        response.setMessage(message);
        response.setMissingHeaders(missingHeaders);
        response.setErrors(errors);
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getImported() {
        return imported;
    }

    public void setImported(int imported) {
        this.imported = imported;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getMissingHeaders() {
        return missingHeaders;
    }

    public void setMissingHeaders(List<String> missingHeaders) {
        this.missingHeaders = missingHeaders == null ? Collections.emptyList() : missingHeaders;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors == null ? Collections.emptyList() : errors;
    }
}
