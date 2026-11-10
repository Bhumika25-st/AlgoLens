package com.algolens.algorithm;

/**
 * High-level algorithm domains supported by AlgoLens.
 */
public enum AlgorithmCategory {
    SORTING("Sorting"),
    SEARCHING("Searching"),
    GRAPH("Graph Traversal");

    private final String displayName;

    AlgorithmCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
