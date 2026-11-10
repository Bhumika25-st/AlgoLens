package com.algolens.model;

/**
 * Dataset distribution presets for generating input arrays.
 */
public enum ArrayPreset {
    RANDOM("Random"),
    NEARLY_SORTED("Nearly Sorted"),
    REVERSED("Reversed"),
    FEW_UNIQUE("Few Unique");

    private final String displayName;

    ArrayPreset(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
