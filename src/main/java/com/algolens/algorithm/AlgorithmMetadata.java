package com.algolens.algorithm;

import java.util.Objects;

/**
 * Immutable metadata describing an algorithm's identity, complexity characteristics,
 * and summary for display in the UI.
 */
public record AlgorithmMetadata(
        String id,
        String displayName,
        AlgorithmCategory category,
        String bestTimeComplexity,
        String averageTimeComplexity,
        String worstTimeComplexity,
        String spaceComplexity,
        String description) {

    public AlgorithmMetadata {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(bestTimeComplexity, "bestTimeComplexity must not be null");
        Objects.requireNonNull(averageTimeComplexity, "averageTimeComplexity must not be null");
        Objects.requireNonNull(worstTimeComplexity, "worstTimeComplexity must not be null");
        Objects.requireNonNull(spaceComplexity, "spaceComplexity must not be null");
        description = description != null ? description : "";
    }
}
