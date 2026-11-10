package com.algolens.algorithm;

/**
 * Unchecked signal thrown cooperatively by algorithm step contexts when an active
 * visualization run is cancelled or reset from the UI controller.
 */
public class ExecutionCancelledException extends RuntimeException {

    public ExecutionCancelledException() {
        super("Algorithm execution cancelled");
    }
}
