package service;

import api.ApiResponse;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Singleton store managing idempotency records.
 * Caches successful responses alongside a fingerprint of the incoming request payload.
 */
public final class IdempotencyStore {

    private static volatile IdempotencyStore instance;
    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    private IdempotencyStore() {
    }

    public static IdempotencyStore getInstance() {
        if (instance == null) {
            synchronized (IdempotencyStore.class) {
                if (instance == null) {
                    instance = new IdempotencyStore();
                }
            }
        }
        return instance;
    }

    public Optional<IdempotencyRecord> get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(key.trim()));
    }

    /**
     * Stores successful response for the given key and payload fingerprint.
     * Only stores successful HTTP responses (2xx).
     */
    public void store(String key, String fingerprint, ApiResponse response) {
        if (key == null || key.trim().isEmpty() || response == null) {
            return;
        }
        if (response.isSuccess()) {
            store.put(key.trim(), new IdempotencyRecord(fingerprint, response));
        }
    }

    public void clear() {
        store.clear();
    }
}
