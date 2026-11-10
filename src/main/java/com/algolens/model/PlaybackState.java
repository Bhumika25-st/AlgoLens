package com.algolens.model;

/**
 * Lifecycle state of the visualization execution controller.
 */
public enum PlaybackState {
    IDLE("Ready"),
    RUNNING("Running"),
    PAUSED("Paused"),
    COMPLETED("Completed");

    private final String displayName;

    PlaybackState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
