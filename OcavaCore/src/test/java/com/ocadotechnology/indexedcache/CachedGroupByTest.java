/*
 * Copyright © 2017-2025 Ocado (Ocava)
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
package com.ocadotechnology.indexedcache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;
import com.ocadotechnology.indexedcache.OneToManyIndexTest.LocationState;

class CachedGroupByTest {

    private IndexedImmutableObjectCache<TestState, TestState> cache;
    private CachedGroupBy<TestState, CoordinateLikeTestObject, Integer> groupByAggregation;

    @BeforeEach
    void init() {
        cache = IndexedImmutableObjectCache.createHashMapBackedCache();
        groupByAggregation = cache.cacheGroupBy(testState -> testState.location, Collectors.summingInt(t -> t.value));
    }

    @Test
    void get_whenMultipleValuesForGroup_thenReturnCorrectAggregation() {
        ImmutableSet<TestState> states = ImmutableSet.of(
                new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN, 1),
                new TestState(Id.create(2), CoordinateLikeTestObject.ORIGIN, 3),
                new TestState(Id.create(3), CoordinateLikeTestObject.ORIGIN, 5));

        cache.addAll(states);
        assertThat(groupByAggregation.get(CoordinateLikeTestObject.ORIGIN)).isEqualTo(9);
    }

    @Test
    void get_whenNoElementsForGroup_thenReturnEmptyAggregation() {
        assertThat(groupByAggregation.get(CoordinateLikeTestObject.ORIGIN)).isEqualTo(0);
    }

    @Test
    void get_whenElementRemoved_thenElementRemovedFromAggregation() {
        TestState stateOne = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN, 1);
        TestState stateTwo = new TestState(Id.create(2), CoordinateLikeTestObject.ORIGIN, 3);

        cache.addAll(ImmutableSet.of(stateOne, stateTwo));
        cache.delete(stateTwo.getId());

        assertThat(groupByAggregation.get(CoordinateLikeTestObject.ORIGIN)).isEqualTo(1);
    }

    @Test
    void get_whenElementGroupValueChanges_thenElementRemovedFromAggregation() {
        TestState stateOne = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN, 1);
        TestState stateTwo = new TestState(Id.create(2), CoordinateLikeTestObject.ORIGIN, 3);

        cache.addAll(ImmutableSet.of(stateOne, stateTwo));

        TestState updatedStateTwo = new TestState(Id.create(2), CoordinateLikeTestObject.create(10, 10), 3);
        cache.update(stateTwo, updatedStateTwo);

        assertThat(groupByAggregation.get(CoordinateLikeTestObject.ORIGIN)).isEqualTo(1);
    }

    @Test
    void get_whenMultipleGroups_thenEachGroupHasCorrectAggregation() {

        CoordinateLikeTestObject otherCoordinate = CoordinateLikeTestObject.create(1, 2);

        ImmutableSet<TestState> states = ImmutableSet.of(
                new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN, 1),
                new TestState(Id.create(2), CoordinateLikeTestObject.ORIGIN, 3),
                new TestState(Id.create(3), otherCoordinate, 5));

        cache.addAll(states);
        assertThat(groupByAggregation.get(CoordinateLikeTestObject.ORIGIN)).isEqualTo(4);
        assertThat(groupByAggregation.get(otherCoordinate)).isEqualTo(5);
    }

    //region Snapshot tests for CachedGroupBy

    @Test
    void snapshot_whenGroupByAggregationIsEmpty_returnsEmptySnapshot() {
        assertThat(groupByAggregation.snapshot()).isEmpty();
    }

    @Test
    void snapshot_whenGroupByAggregationIsAddedTo_returnsSnapshotWithThatElement() {
        TestState testState = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN, 42);
        cache.add(testState);

        assertThat(groupByAggregation.snapshot()).isEqualTo(ImmutableMap.of(testState.location, testState.value));
    }

    @Test
    void snapshot_whenGroupByAggregationIsRemovedFrom_returnsSnapshotWithoutThatElement() {
        TestState stateOne = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN, 1);
        TestState stateTwo = new TestState(Id.create(2), CoordinateLikeTestObject.create(1, 1), 2);
        cache.addAll(ImmutableSet.of(stateOne, stateTwo));
        groupByAggregation.snapshot(); // So call below is not first call

        cache.delete(stateOne.getId());

        assertThat(groupByAggregation.snapshot()).isEqualTo(ImmutableMap.of(stateTwo.location, stateTwo.value));
    }

    @Test
    void snapshot_whenNoChanges_thenSameObjectReturned() {
        TestState testState = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN, 42);
        cache.add(testState);

        Object firstSnapshot = groupByAggregation.snapshot();
        Object secondSnapshot = groupByAggregation.snapshot();

        assertThat(firstSnapshot).isSameAs(secondSnapshot);
    }

    @Test
    void snapshot_whenGroupByAggregationAddedTo_thenNewObjectReturned() {
        Object firstSnapshot = groupByAggregation.snapshot();

        TestState testState = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN, 5);
        cache.add(testState);
        Object secondSnapshot = groupByAggregation.snapshot();

        assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
    }

    @Test
    void snapshot_whenGroupByAggregationRemovedFrom_thenNewObjectReturned() {
        TestState testState = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN, 5);
        cache.add(testState);

        Object firstSnapshot = groupByAggregation.snapshot();

        cache.delete(testState.getId());

        Object secondSnapshot = groupByAggregation.snapshot();
        assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
    }

    //endregion

    private static final class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final CoordinateLikeTestObject location;
        private final int value;

        private TestState(Id<TestState> id, CoordinateLikeTestObject location, int value) {
            super(id);
            this.location = location;
            this.value = value;
        }

        @Override
        public CoordinateLikeTestObject getLocation() {
            return location;
        }
    }
}
