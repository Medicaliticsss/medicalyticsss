package com.example.frontend.models;

import java.util.List;

public class DictionaryFetchResult {

    private final List<TestTypeEntry> entries;
    private final String errorMessage;

    public DictionaryFetchResult(List<TestTypeEntry> entries, String errorMessage) {
        this.entries = entries;
        this.errorMessage = errorMessage;
    }

    public List<TestTypeEntry> getEntries() {
        return entries;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return errorMessage == null;
    }
}
