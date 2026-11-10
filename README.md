# AlgoLens

Desktop-based Algorithm Visualizer built with Core Java, Java Swing/AWT, and FlatLaf Dark Mode.

## Modular Package Structure

- `com.algolens.app` — Application bootstrap (`AlgoLensApp`) initializing `FlatDarkLaf` and launching the UI on the Swing Event Dispatch Thread.
- `com.algolens.model` — Core data structures (`ArrayModel`, `StepSnapshot`, `ElementState`, `ArrayPreset`, `PlaybackState`), including validation and parsing for custom comma-separated integer arrays and search range/target tracking.
- `com.algolens.algorithm` — Extensible algorithm contracts (`AbstractAlgorithm`, `AlgorithmMetadata`, `AlgorithmCategory`, `AlgorithmRegistry`, `ExecutionCancelledException`).
- `com.algolens.algorithm.sorting` — Sorting engine (`AbstractSortingAlgorithm`, `SortingStepContext`, `BubbleSort`, `QuickSort`).
- `com.algolens.algorithm.searching` — Searching engine (`AbstractSearchingAlgorithm`, `SearchStepContext`, `LinearSearch`, `BinarySearch`).
- `com.algolens.controller` — Background execution controller (`VisualizationController`, `ExecutionListener`) built on `SwingWorker` and `ReentrantLock`/`Condition` for safe play, pause, single-step, reset, custom dataset loading, target updates, and live speed changes.
- `com.algolens.view` — FlatLaf-styled Swing/AWT UI framework (`MainWindow`, `ControlToolbarPanel`, `SortingVisualizerPanel`, `CodeViewerPanel`, `StatusMetricsPanel`, `AlgoLensTheme`).

## Build & Run

Requires JDK 17+ (tested on OpenJDK 25). Includes `pom.xml` (for Maven builds with `com.formdev:flatlaf:3.8`) and `lib/flatlaf-3.8.jar` for direct execution via `run.ps1`.

Run tests (`SortingEngineTest` and `SearchEngineTest`):
```powershell
.\run.ps1 -TestOnly
```

Launch the desktop application:
```powershell
.\run.ps1
```

Or with Maven:
```powershell
mvn compile exec:java
```
