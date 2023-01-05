/*
 * Copyright © 2017-2023 Ocado (Ocava)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocadotechnology.utils;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table.Cell;

/**
 * A class to facilitate dividing a discrete positive quantity amongst different groups according
 * to their weights.
 * <br> When an exact assignment cannot be achieved due of the discreteness of the quantity,
 * the groups with the highest values will be rounded up.
 * @param <E> the type of the groups the quantity will be spit amongst
 */
public class NonNegativeIntegerSplitterByWeights<E> {
    private final Comparator<Entry<E, Double>> valueComparator = Entry.comparingByValue();

    private ImmutableMap<E, Double> proportions;

    /**
     * Create a splitter based on {@link Double} weights.
     * The ordering of the {@link ImmutableMap} may determine the groups to round up in case of weight tie
     * (note that {@link ImmutableMap} is deterministic).
     *
     * @param weights assigned to each group.
     */
    public NonNegativeIntegerSplitterByWeights(ImmutableMap<E, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(w -> w).sum();
        proportions = ImmutableMapFactory.createWithNewValues(weights, weight -> weight / totalWeight);
    }

    /**
     * Create a splitter based on {@link Double} weights.
     * A reverse comparison between the groups may determine the groups to round up in case of weight tie.
     * In case of tie also in the direct comparison, the order of the {@link ImmutableMap} will determine the split
     * (note that {@link ImmutableMap} is deterministic).
     *
     * @param weights assigned to each group.
     * @param <E> the type of the groups the quantity will be split amongst
     */
    public static <E extends Comparable<? super E>> NonNegativeIntegerSplitterByWeights<E> createWithSorting(
            ImmutableMap<E, Double> weights) {

        ImmutableList<E> orderedKeys = weights.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .collect(ImmutableList.toImmutableList());

        return new NonNegativeIntegerSplitterByWeights<>(ImmutableMapFactory.createFromKeys(orderedKeys, weights::get));
    }

    /**
     * Create a splitter based on {@link Integer} weights.
     * The ordering of the {@link ImmutableMap} may determine the groups to round up in case of weight tie
     * (note that {@link ImmutableMap} is deterministic).
     *
     * @param weights assigned to each group.
     * @param <E> the type of the groups the quantity will be split amongst
     */
    public static <E> NonNegativeIntegerSplitterByWeights<E> createFromInts(ImmutableMap<E, Integer> weights) {
        return new NonNegativeIntegerSplitterByWeights<>(
                ImmutableMapFactory.createWithNewValues(weights, weight -> (double) weight));
    }

    /**
     * Create a splitter based on {@link Integer} weights.
     * A reverse comparison between the groups may determine the groups to round up in case of weight tie.
     * In case of tie also in the direct comparison, the order of the {@link ImmutableMap} will determine the split
     * (note that {@link ImmutableMap} is deterministic).
     *
     * @param weights assigned to each group.
     * @param <E> the type of the groups the quantity will be split amongst
     */
    public static <E extends Comparable<? super E>> NonNegativeIntegerSplitterByWeights<E> createFromIntsWithSorting(
            ImmutableMap<E, Integer> weights) {

        ImmutableList<E> orderedKeys = weights.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .collect(ImmutableList.toImmutableList());

        return createFromInts(ImmutableMapFactory.createFromKeys(orderedKeys, weights::get));
    }

    /**
     * Split quantity based on {@link ImmutableTable} weights.
     * It creates a splitter with integer weights with table cells as keys, splits the quantity
     * and return it in {@link ImmutableTable} format.
     * A reverse comparison between the groups may determine the groups to round up in case of weight tie.
     * In case of tie also in the direct comparison, the order of the {@link ImmutableMap} will determine the split
     * (note that {@link ImmutableMap} is deterministic).
     * The ordering of the {@link ImmutableTable} may determine the groups to round up in case of weight tie.
     *
     * @param weights           assigned to each group.
     * @param numberToSplit     quantity to split.
     * @param <R>               the type of the rows.
     * @param <C>               the type of the columns.
     * @return the quantity split by the table cells.
     */
    public static <R, C> ImmutableTable<R, C, Integer> splitByTableWeights(
            ImmutableTable<R, C, Double> weights,
            int numberToSplit) {

        ImmutableMap<Cell<R, C, Double>, Double> weightMap = weights.cellSet().stream()
                .collect(ImmutableMap.toImmutableMap(
                        Function.identity(),
                        Cell::getValue));

        ImmutableMap<Cell<R, C, Double>, Integer> split =
                new NonNegativeIntegerSplitterByWeights<>(weightMap).split(numberToSplit);

        return split.keySet().stream()
                .collect(ImmutableTable.toImmutableTable(
                        Cell::getRowKey,
                        Cell::getColumnKey,
                        split::get));
    }

    /**
     * Split quantity based on {@link ImmutableTable} weights.
     * It creates a splitter with integer weights with table cells as keys, splits the quantity
     * and return it in {@link ImmutableTable} format.
     * A reverse comparison between the groups may determine the groups to round up in case of weight tie.
     * In case of tie also in the direct comparison, the order of the {@link ImmutableMap} will determine the split
     * (note that {@link ImmutableMap} is deterministic).
     * The ordering of the {@link ImmutableTable} may determine the groups to round up in case of weight tie.
     *
     * @param weights           assigned to each group.
     * @param numberToSplit     quantity to split.
     * @param <R>               the type of the rows.
     * @param <C>               the type of the columns.
     * @return the quantity split by the table cells.
     */
    public static <R, C> ImmutableTable<R, C, Integer> splitByIntegerTableWeights(
            ImmutableTable<R, C, Integer> weights,
            int numberToSplit) {

        ImmutableMap<Cell<R, C, Integer>, Integer> weightMap = weights.cellSet().stream()
                .collect(ImmutableMap.toImmutableMap(
                        Function.identity(),
                        Cell::getValue));

        ImmutableMap<Cell<R, C, Integer>, Integer> split =
                NonNegativeIntegerSplitterByWeights.createFromInts(weightMap).split(numberToSplit);

        return split.keySet().stream()
                .collect(ImmutableTable.toImmutableTable(
                        Cell::getRowKey,
                        Cell::getColumnKey,
                        split::get));
    }

    /**
     * Split a positive quantity amongst groups according to their weights.
     * @param numberToSplit     the non negative quantity to split
     * @return the group to its share of the numberToSplit map
     * @throws IllegalArgumentException if numberToSplit is negative
     */
    public ImmutableMap<E, Integer> split(int numberToSplit) {
        Preconditions.checkArgument(numberToSplit >= 0, "Quantity to split must be non negative");

        if (proportions.isEmpty()) {
            return ImmutableMap.of();
        }

        ImmutableMap<E, Double> idealAmounts = ImmutableMapFactory.createWithNewValues(
                proportions,
                p -> p * numberToSplit);

        ImmutableMap<E, Integer> roundedDownAmounts = ImmutableMapFactory.createWithNewValues(
                idealAmounts,
                idealAmount -> (int) Math.floor(idealAmount));

        return assignRemainder(numberToSplit, idealAmounts, roundedDownAmounts);
    }

    private ImmutableMap<E, Integer> assignRemainder(
            int totalToBeAssigned,
            ImmutableMap<E, Double> idealAmounts,
            ImmutableMap<E, Integer> roundedDownAmounts) {
        int summedValues = roundedDownAmounts.values().stream().mapToInt(Integer::intValue).sum();
        int totalRemainder = totalToBeAssigned - summedValues;

        ImmutableMap<E, Double> remainderByItem = ImmutableMapFactory.createWithNewValues(
                idealAmounts,
                (item, idealAmount) -> idealAmount - roundedDownAmounts.get(item));

        ImmutableSet<E> itemsToAddTo = remainderByItem.entrySet().stream()
                .sorted(valueComparator.reversed())
                .limit(totalRemainder)
                .map(Entry::getKey)
                .collect(ImmutableSet.toImmutableSet());

        return ImmutableMapFactory.createWithNewValues(
                roundedDownAmounts,
                (item, amount) -> itemsToAddTo.contains(item) ? Integer.valueOf(amount + 1) : amount);
    }
}
