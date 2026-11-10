package com.algolens.controller;

import com.algolens.algorithm.AbstractAlgorithm;
import com.algolens.algorithm.ExecutionCancelledException;
import com.algolens.algorithm.searching.AbstractSearchingAlgorithm;
import com.algolens.algorithm.searching.SearchStepContext;
import com.algolens.algorithm.sorting.AbstractSortingAlgorithm;
import com.algolens.algorithm.sorting.SortingStepContext;
import com.algolens.model.ArrayModel;
import com.algolens.model.ArrayPreset;
import com.algolens.model.PlaybackState;
import com.algolens.model.StepSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Coordinates sorting and searching algorithm execution on a background {@link SwingWorker}
 * thread while safely supporting play, pause, single-step, reset, target changes, and live
 * speed adjustments without blocking the EDT.
 */
public class VisualizationController {

    public static final int MIN_DELAY_MS = 2;
    public static final int MAX_DELAY_MS = 500;
    public static final int DEFAULT_DELAY_MS = 65;

    private final ArrayModel model;
    private final List<ExecutionListener> listeners = new ArrayList<>();
    private final ReentrantLock gateLock = new ReentrantLock();
    private final Condition gateCondition = gateLock.newCondition();
    private final AtomicLong runGeneration = new AtomicLong(0);

    private AbstractAlgorithm currentAlgorithm;
    private PlaybackState playbackState = PlaybackState.IDLE;
    private AlgorithmExecutionWorker activeWorker;
    private volatile int stepDelayMs = DEFAULT_DELAY_MS;
    private boolean paused;
    private int stepPermits;

    public VisualizationController(ArrayModel model, AbstractAlgorithm initialAlgorithm) {
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.currentAlgorithm = Objects.requireNonNull(initialAlgorithm, "initialAlgorithm must not be null");
        this.model.setActiveCategory(initialAlgorithm.getCategory());
    }

    public void addListener(ExecutionListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
    }

    public void removeListener(ExecutionListener listener) {
        listeners.remove(listener);
    }

    public ArrayModel getModel() {
        return model;
    }

    public AbstractAlgorithm getCurrentAlgorithm() {
        return currentAlgorithm;
    }

    public PlaybackState getPlaybackState() {
        return playbackState;
    }

    public int getStepDelayMs() {
        return stepDelayMs;
    }

    public int getSearchTarget() {
        return model.getSearchTarget();
    }

    /**
     * Selects a new algorithm (sorting or searching), cancelling any active visualization
     * and ensuring sorted input when required (e.g., Binary Search).
     */
    public void setAlgorithm(AbstractAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        cancelActiveWorker();
        this.currentAlgorithm = algorithm;
        model.setActiveCategory(algorithm.getCategory());
        prepareDatasetForAlgorithm(algorithm, false);
        setPlaybackState(PlaybackState.IDLE);
        for (ExecutionListener listener : listeners) {
            listener.onAlgorithmChanged(algorithm);
        }
    }

    /**
     * Updates the target integer for searching algorithms, cancelling any in-flight search.
     */
    public void setSearchTarget(int target) {
        if (playbackState == PlaybackState.RUNNING || playbackState == PlaybackState.PAUSED) {
            cancelActiveWorker();
            model.resetToInitial();
            setPlaybackState(PlaybackState.IDLE);
        } else if (playbackState == PlaybackState.COMPLETED) {
            model.resetToInitial();
            setPlaybackState(PlaybackState.IDLE);
        }
        model.setSearchTarget(target);
    }

    /**
     * Updates the per-step animation delay dynamically, waking the worker immediately if sleeping.
     */
    public void setStepDelayMs(int delayMs) {
        int clamped = Math.max(MIN_DELAY_MS, Math.min(MAX_DELAY_MS, delayMs));
        this.stepDelayMs = clamped;
        gateLock.lock();
        try {
            gateCondition.signalAll();
        } finally {
            gateLock.unlock();
        }
        for (ExecutionListener listener : listeners) {
            listener.onSpeedChanged(clamped);
        }
    }

    /**
     * Starts or resumes continuous animated execution.
     */
    public void play() {
        if (playbackState == PlaybackState.RUNNING) {
            return;
        }
        if (playbackState == PlaybackState.PAUSED && activeWorker != null) {
            gateLock.lock();
            try {
                paused = false;
                stepPermits = 0;
                gateCondition.signalAll();
            } finally {
                gateLock.unlock();
            }
            setPlaybackState(PlaybackState.RUNNING);
            return;
        }

        if (playbackState == PlaybackState.COMPLETED) {
            model.resetToInitial();
        }
        prepareDatasetForAlgorithm(currentAlgorithm, true);
        startWorker(false);
    }

    /**
     * Pauses an active visualization run at the current step boundary.
     */
    public void pause() {
        if (playbackState != PlaybackState.RUNNING || activeWorker == null) {
            return;
        }
        gateLock.lock();
        try {
            paused = true;
            stepPermits = 0;
            gateCondition.signalAll();
        } finally {
            gateLock.unlock();
        }
        setPlaybackState(PlaybackState.PAUSED);
    }

    /**
     * Toggles between Play and Pause states.
     */
    public void togglePlayPause() {
        if (playbackState == PlaybackState.RUNNING) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Advances execution by exactly one step, starting in paused mode if currently idle.
     */
    public void stepForward() {
        if (playbackState == PlaybackState.RUNNING) {
            pause();
            return;
        }
        if (playbackState == PlaybackState.PAUSED && activeWorker != null) {
            gateLock.lock();
            try {
                paused = true;
                stepPermits++;
                gateCondition.signalAll();
            } finally {
                gateLock.unlock();
            }
            return;
        }

        if (playbackState == PlaybackState.COMPLETED) {
            model.resetToInitial();
        }
        prepareDatasetForAlgorithm(currentAlgorithm, true);
        startWorker(true);
    }

    /**
     * Cancels execution and restores the initial dataset.
     */
    public void reset() {
        cancelActiveWorker();
        model.resetToInitial();
        setPlaybackState(PlaybackState.IDLE);
    }

    /**
     * Cancels execution and generates a new array with the given size and preset.
     */
    public void generateNewArray(int size, ArrayPreset preset) {
        cancelActiveWorker();
        model.generate(size, preset);
        prepareDatasetForAlgorithm(currentAlgorithm, true);
        setPlaybackState(PlaybackState.IDLE);
    }

    /**
     * Cancels any active execution and loads a user-supplied custom array, resetting to IDLE.
     */
    public void loadCustomArray(int[] customValues) {
        cancelActiveWorker();
        model.setCustomValues(customValues);
        prepareDatasetForAlgorithm(currentAlgorithm, true);
        setPlaybackState(PlaybackState.IDLE);
    }

    /**
     * Releases background worker threads cleanly during window shutdown.
     */
    public void shutdown() {
        cancelActiveWorker();
    }

    private void prepareDatasetForAlgorithm(AbstractAlgorithm algorithm, boolean onlyIfRequiredSorted) {
        if (algorithm instanceof AbstractSearchingAlgorithm searchAlg && searchAlg.requiresSortedInput()) {
            if (!model.isInitialSorted() || !onlyIfRequiredSorted) {
                model.ensureSortedAscending(algorithm.getDisplayName());
            }
        } else if (!onlyIfRequiredSorted) {
            model.resetToInitial();
        }
    }

    private void startWorker(boolean startPausedSingleStep) {
        cancelActiveWorker();

        long runId = runGeneration.incrementAndGet();
        gateLock.lock();
        try {
            paused = startPausedSingleStep;
            stepPermits = startPausedSingleStep ? 1 : 0;
        } finally {
            gateLock.unlock();
        }

        int[] workingCopy = model.getValuesCopy();
        int targetSnapshot = model.getSearchTarget();
        AlgorithmExecutionWorker worker =
                new AlgorithmExecutionWorker(runId, currentAlgorithm, workingCopy, targetSnapshot);
        this.activeWorker = worker;
        setPlaybackState(startPausedSingleStep ? PlaybackState.PAUSED : PlaybackState.RUNNING);
        worker.execute();
    }

    private void cancelActiveWorker() {
        runGeneration.incrementAndGet();
        AlgorithmExecutionWorker previous = this.activeWorker;
        this.activeWorker = null;

        gateLock.lock();
        try {
            paused = false;
            stepPermits = 0;
            if (previous != null) {
                previous.requestCancel();
            }
            gateCondition.signalAll();
        } finally {
            gateLock.unlock();
        }
    }

    private void setPlaybackState(PlaybackState newState) {
        if (this.playbackState != newState) {
            this.playbackState = newState;
            for (ExecutionListener listener : listeners) {
                listener.onPlaybackStateChanged(newState);
            }
        }
    }

    /**
     * Background SwingWorker driving one algorithm execution run and publishing snapshots to the EDT.
     */
    private final class AlgorithmExecutionWorker extends SwingWorker<Void, StepSnapshot> {

        private final long workerRunId;
        private final AbstractAlgorithm algorithm;
        private final int[] workingArray;
        private final int searchTarget;
        private final AtomicBoolean workerCancelled = new AtomicBoolean(false);
        private volatile StepSnapshot lastPublishedSnapshot;

        private AlgorithmExecutionWorker(
                long workerRunId,
                AbstractAlgorithm algorithm,
                int[] workingArray,
                int searchTarget) {
            this.workerRunId = workerRunId;
            this.algorithm = algorithm;
            this.workingArray = workingArray;
            this.searchTarget = searchTarget;
        }

        private void requestCancel() {
            workerCancelled.set(true);
            cancel(true);
        }

        @Override
        protected Void doInBackground() {
            try {
                if (algorithm instanceof AbstractSortingAlgorithm sortingAlg) {
                    SortingStepContext context =
                            new SortingStepContext(workingArray.length, this::handleEmittedStep);
                    sortingAlg.sort(workingArray, context);
                } else if (algorithm instanceof AbstractSearchingAlgorithm searchingAlg) {
                    SearchStepContext context =
                            new SearchStepContext(workingArray.length, searchTarget, this::handleEmittedStep);
                    searchingAlg.search(workingArray, searchTarget, context);
                }
            } catch (ExecutionCancelledException ignored) {
                // Cooperative cancellation requested by controller.
            }
            return null;
        }

        private void handleEmittedStep(StepSnapshot snapshot) {
            awaitStepGate(snapshot.getStepNumber());
            lastPublishedSnapshot = snapshot;
            publish(snapshot);
        }

        private void awaitStepGate(long stepNumber) {
            boolean isFirstStep = (stepNumber == 1);
            long delayStartNanos = System.nanoTime();

            gateLock.lock();
            try {
                while (true) {
                    if (isCancelledOrStale()) {
                        throw new ExecutionCancelledException();
                    }

                    if (paused) {
                        if (stepPermits > 0) {
                            stepPermits--;
                            return;
                        }
                        try {
                            gateCondition.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new ExecutionCancelledException();
                        }
                        delayStartNanos = System.nanoTime();
                        continue;
                    }

                    if (isFirstStep) {
                        return;
                    }

                    long targetDelayNanos = TimeUnit.MILLISECONDS.toNanos(stepDelayMs);
                    long elapsedNanos = System.nanoTime() - delayStartNanos;
                    long remainingNanos = targetDelayNanos - elapsedNanos;
                    if (remainingNanos <= 0) {
                        return;
                    }

                    try {
                        long chunkNanos = Math.min(remainingNanos, TimeUnit.MILLISECONDS.toNanos(25));
                        gateCondition.awaitNanos(chunkNanos);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ExecutionCancelledException();
                    }
                }
            } finally {
                gateLock.unlock();
            }
        }

        private boolean isCancelledOrStale() {
            return workerCancelled.get()
                    || isCancelled()
                    || Thread.currentThread().isInterrupted()
                    || workerRunId != runGeneration.get();
        }

        @Override
        protected void process(List<StepSnapshot> chunks) {
            if (!SwingUtilities.isEventDispatchThread() || isCancelledOrStale() || chunks.isEmpty()) {
                return;
            }
            StepSnapshot latest = chunks.get(chunks.size() - 1);
            model.applySnapshot(latest);
        }

        @Override
        protected void done() {
            if (isCancelledOrStale()) {
                return;
            }
            if (lastPublishedSnapshot != null && model.getStepNumber() < lastPublishedSnapshot.getStepNumber()) {
                model.applySnapshot(lastPublishedSnapshot);
            }
            activeWorker = null;
            setPlaybackState(PlaybackState.COMPLETED);
        }
    }
}
