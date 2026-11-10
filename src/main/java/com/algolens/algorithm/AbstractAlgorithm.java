package com.algolens.algorithm;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base abstract class for all visualizable algorithms in AlgoLens.
 * Concrete subclasses supply descriptive metadata and synchronized source/pseudocode lines.
 */
public abstract class AbstractAlgorithm {

    private final AlgorithmMetadata metadata;
    private final List<String> codeLines;

    protected AbstractAlgorithm(AlgorithmMetadata metadata, List<String> codeLines) {
        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(codeLines, "codeLines must not be null");
        this.codeLines = Collections.unmodifiableList(List.copyOf(codeLines));
    }

    public AlgorithmMetadata getMetadata() {
        return metadata;
    }

    public String getId() {
        return metadata.id();
    }

    public String getDisplayName() {
        return metadata.displayName();
    }

    public AlgorithmCategory getCategory() {
        return metadata.category();
    }

    /**
     * Returns the immutable list of 1-based display lines rendered in the synchronized code pane.
     */
    public List<String> getCodeLines() {
        return codeLines;
    }

    @Override
    public String toString() {
        return metadata.displayName();
    }
}
