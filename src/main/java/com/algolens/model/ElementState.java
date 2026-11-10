package com.algolens.model;

/**
 * Visual and logical state of an array element during algorithm visualization.
 */
public enum ElementState {
    /** Element is in its baseline idle/active search-space state. */
    DEFAULT("Default"),
    /** Element is currently being compared with another element, pivot, or target. */
    COMPARING("Comparing"),
    /** Element is currently being swapped or repositioned. */
    SWAPPING("Swapping"),
    /** Element is the active pivot in a divide-and-conquer algorithm. */
    PIVOT("Pivot"),
    /** Element is confirmed to be in its final sorted position. */
    SORTED("Sorted"),
    /** Represents the target value state in searching mode. */
    TARGET("Target"),
    /** Element has been eliminated from the active search space. */
    DISCARDED("Discarded"),
    /** Element matches the search target at its final located index. */
    FOUND("Found");

    private final String displayName;

    ElementState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
