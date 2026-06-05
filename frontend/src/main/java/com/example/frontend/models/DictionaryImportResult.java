package com.example.frontend.models;

public class DictionaryImportResult {

    private int syncedCount;
    private String message;

    public int getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(int syncedCount) {
        this.syncedCount = syncedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
