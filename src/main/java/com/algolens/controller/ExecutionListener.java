package com.algolens.controller;

import com.algolens.algorithm.AbstractAlgorithm;
import com.algolens.model.PlaybackState;

/**
 * Listener notified on the Swing EDT when execution controller state or selected algorithm changes.
 */
public interface ExecutionListener {

    default void onPlaybackStateChanged(PlaybackState state) {
    }

    default void onAlgorithmChanged(AbstractAlgorithm algorithm) {
    }

    default void onSpeedChanged(int delayMs) {
    }
}
