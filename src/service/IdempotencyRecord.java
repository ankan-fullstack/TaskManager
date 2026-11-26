package service;

import api.ApiResponse;

/**
 * Encapsulates a cached idempotent operation response and its request fingerprint.
 */
public class IdempotencyRecord {
    private final String fingerprint;
    private final ApiResponse response;

    public IdempotencyRecord(String fingerprint, ApiResponse response) {
        this.fingerprint = fingerprint;
        this.response = response;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public ApiResponse getResponse() {
        return response;
    }
}
