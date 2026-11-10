# Project Documentation: AlgoLens (Interactive Algorithm Visualizer & Analysis Platform)

## 1. Abstract
**AlgoLens** is an interactive desktop-based educational software designed to bridge the gap between theoretical Data Structures and Algorithms (DSA) and visual comprehension. Developed in Core Java using Java Swing for custom graphical rendering and multithreading for step-by-step animation control, AlgoLens allows students and educators to visualize sorting and graph algorithms in real-time. By incorporating code synchronization, execution metrics tracking, and persistent MySQL database storage via JDBC, AlgoLens serves as an end-to-end practical learning and evaluation tool.

---

## 2. Introduction
In computer science education, complex algorithms are frequently taught using static textbook diagrams and raw code snippets. This abstraction makes it difficult for students to visualize dynamic state changes, pointer movements, and time complexities. 
AlgoLens solves this problem by rendering algorithms dynamically. Users can control execution flow (Play, Pause, Step-Forward, Step-Back), adjust speed dynamically, and observe real-time color-coded state transitions (comparisons, swaps, completion).

---

## 3. Problem Statement
Traditional methods of learning algorithms lack interactive feedback and real-time visualization. Students struggle to grasp why certain sorting algorithms have $O(N \log N)$ efficiency versus $O(N^2)$ without seeing element-by-element comparisons. There is a need for a lightweight, extensible, offline-capable desktop utility that visualizes algorithm execution steps while tracking metrics like comparison counts and execution time.

---

## 4. Objectives & Scope
### Objectives
* Provide real-time, animated visualization of core sorting and graph algorithms.
* Implement robust multithreading to ensure the graphical user interface (GUI) remains responsive during animations.
* Maintain clean Object-Oriented design patterns (Strategy Pattern) to allow seamless addition of new algorithms.
* Store historical performance metrics and quiz scores securely in a MySQL database.

### Scope
* **Phase 1:** Core sorting visualizer (Bubble Sort, Quick Sort), control panel, custom JPanel rendering, and code synchronization.
* **Phase 2 (Future Scope):** Advanced graph traversal algorithms (BFS, DFS, Dijkstra) and user quiz modules.

---

## 5. Existing vs. Proposed Systems

| Feature | Existing Systems (Online Web Visualizers) | Proposed System (AlgoLens - Java Desktop) |
| :--- | :--- | :--- |
| **Connectivity** | Requires internet access and modern web browser | Fully offline desktop application (Core Java) |
| **Performance** | Can suffer from DOM rendering lag during heavy loops | High-performance native rendering via Java Swing (`JPanel`) |
| **Architecture** | Often tightly coupled JavaScript code | Clean modular architecture (MVC + Strategy Pattern) |
| **Data Persistence** | LocalStorage or none | Relational database storage (MySQL via JDBC) |

---

## 6. System Architecture & Package Structure
AlgoLens follows a strict **Model-View-Controller (MVC)** and **Strategy Design Pattern** architecture to ensure modularity and maintainability:

```text
com.algolens/
│
├── model/               # Data structures (ArrayElement, AlgorithmStep, GraphNode)
├── algorithm/           # Algorithm logic implementations
│     ├── base/          # Abstract Algorithm class & Strategy interface
│     ├── sorting/       # BubbleSort.java, QuickSort.java
└── view/                # Swing UI components (MainFrame, VisualizerPanel, ControlPanel)
```

---

## 7. Module Descriptions
1. **Model Module (`com.algolens.model`):** Encapsulates data states such as individual array elements, their current visual status (default, comparing, swapped, sorted), and snapshot steps for animation.
2. **Algorithm Module (`com.algolens.algorithm`):** Implements sorting and searching logic decoupled from the UI. Uses the Strategy pattern so each algorithm produces a sequence of renderable states.
3. **View Module (`com.algolens.view`):** Houses the Java Swing user interface, including custom `JPanel` renderers that override `paintComponent()` to draw dynamic color-coded bars and control toolbars.
4. **Controller / Threading Module:** Manages background execution threads (`SwingWorker` / `Thread.sleep` delays) to pause and resume animations smoothly without locking the Event Dispatch Thread (EDT).

---

## 8. Implementation Details (Core Concepts Used)
* **Object-Oriented Programming (OOP):** Encapsulation of array elements, Inheritance for UI panels, and Polymorphism via sorting algorithm interfaces.
* **Multithreading:** Prevents UI freezing by running sorting loops in background worker threads while updating GUI states safely via `SwingUtilities.invokeLater`.
* **Collections Framework:** Heavy use of `ArrayList` and custom step queues to record and playback algorithm states.
* **Exception Handling:** Custom exception handlers for invalid input sizes or empty array states.

---

## 9. Testing & Quality Assurance
* **Unit Testing:** Verified individual sorting algorithms against edge cases (already sorted arrays, reverse-sorted arrays, arrays with duplicate elements).
* **Concurrency Testing:** Tested rapid clicking of Pause/Resume and Speed Slider adjustments to ensure no race conditions or thread deadlocks occur.
* **UI Responsiveness Testing:** Ensured 60 FPS rendering during sorting of up to 100 elements.

---

## 10. Conclusion & Future Scope
AlgoLens successfully bridges the gap between theoretical algorithm design and practical application development. It provides students with an interactive sandbox to master Data Structures and Algorithms. 
**Future Scope:** Expanding the platform to support pathfinding algorithms (A*, Dijkstra) and integrating automated automated quiz generation.

---

## 11. Sample Viva Questions & Answers
* **Q: Why did you choose Java Swing instead of a web framework?**
  * *A:* Java Swing allowed us to demonstrate deep mastery of Core Java, event-driven programming, custom 2D graphics rendering (`paintComponent`), and thread management in a standalone desktop environment.
* **Q: How do you prevent the UI from freezing during algorithm execution?**
  * *A:* We utilize background threads (`SwingWorker` or dedicated worker threads) combined with controlled execution delays (`Thread.sleep`), ensuring that heavy computational loops do not block the Java Swing Event Dispatch Thread (EDT).
* **Q: Which design pattern did you use for the algorithms?**
  * *A:* We used the **Strategy Pattern**, allowing any sorting algorithm to implement a common interface so the visualizer panel can execute and render them interchangeably.