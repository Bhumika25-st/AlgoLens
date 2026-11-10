package com.algolens.algorithm;

import com.algolens.algorithm.searching.AbstractSearchingAlgorithm;
import com.algolens.algorithm.searching.BinarySearch;
import com.algolens.algorithm.searching.LinearSearch;
import com.algolens.algorithm.sorting.AbstractSortingAlgorithm;
import com.algolens.algorithm.sorting.BubbleSort;
import com.algolens.algorithm.sorting.QuickSort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Central extensible registry for available sorting and searching visualization algorithms.
 * New algorithms can be registered dynamically without modifying UI components.
 */
public class AlgorithmRegistry {

    private final Map<String, AbstractAlgorithm> allAlgorithms = new LinkedHashMap<>();
    private final Map<String, AbstractSortingAlgorithm> sortingAlgorithms = new LinkedHashMap<>();
    private final Map<String, AbstractSearchingAlgorithm> searchingAlgorithms = new LinkedHashMap<>();

    public static AlgorithmRegistry createDefault() {
        AlgorithmRegistry registry = new AlgorithmRegistry();
        registry.registerSortingAlgorithm(new BubbleSort());
        registry.registerSortingAlgorithm(new QuickSort());
        registry.registerSearchingAlgorithm(new LinearSearch());
        registry.registerSearchingAlgorithm(new BinarySearch());
        return registry;
    }

    public void registerSortingAlgorithm(AbstractSortingAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        sortingAlgorithms.put(algorithm.getId(), algorithm);
        allAlgorithms.put(algorithm.getId(), algorithm);
    }

    public void registerSearchingAlgorithm(AbstractSearchingAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        searchingAlgorithms.put(algorithm.getId(), algorithm);
        allAlgorithms.put(algorithm.getId(), algorithm);
    }

    public List<AbstractSortingAlgorithm> getSortingAlgorithms() {
        return Collections.unmodifiableList(new ArrayList<>(sortingAlgorithms.values()));
    }

    public List<AbstractSearchingAlgorithm> getSearchingAlgorithms() {
        return Collections.unmodifiableList(new ArrayList<>(searchingAlgorithms.values()));
    }

    public List<AbstractAlgorithm> getAlgorithmsByCategory(AlgorithmCategory category) {
        Objects.requireNonNull(category, "category must not be null");
        return switch (category) {
            case SORTING -> List.copyOf(sortingAlgorithms.values());
            case SEARCHING -> List.copyOf(searchingAlgorithms.values());
            default -> allAlgorithms.values().stream()
                    .filter(alg -> alg.getCategory() == category)
                    .toList();
        };
    }

    public List<AlgorithmCategory> getActiveCategories() {
        List<AlgorithmCategory> categories = new ArrayList<>();
        if (!sortingAlgorithms.isEmpty()) {
            categories.add(AlgorithmCategory.SORTING);
        }
        if (!searchingAlgorithms.isEmpty()) {
            categories.add(AlgorithmCategory.SEARCHING);
        }
        return Collections.unmodifiableList(categories);
    }

    public Optional<AbstractSortingAlgorithm> findSortingAlgorithm(String id) {
        return Optional.ofNullable(sortingAlgorithms.get(id));
    }

    public Optional<AbstractSearchingAlgorithm> findSearchingAlgorithm(String id) {
        return Optional.ofNullable(searchingAlgorithms.get(id));
    }

    public Optional<AbstractAlgorithm> findAlgorithm(String id) {
        return Optional.ofNullable(allAlgorithms.get(id));
    }
}
