/*
 * Copyright © 2017 Ocado (Ocava)
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

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMultimap;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("An OptionalOneToManyIndex")
class OptionalOneToManyIndexTest {

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        OptionalOneToManyIndex<Coordinate, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOptionalOneToManyIndex(TestState::getLocation);
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        OptionalOneToManyIndex<Coordinate, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOptionalOneToManyIndex() require a type
            // of Function<TestState, Optional<Coordinate>> instead of Function<? super TestState, Optional<Coordinate<>, due
            // to automatic type coercion of the lambda.
            Function<LocationState, Optional<Coordinate>> indexFunction = LocationState::getLocation;
            return cache.addOptionalOneToManyIndex(indexFunction);
        }
    }

    private abstract static class IndexTests {

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private OptionalOneToManyIndex<Coordinate, TestState> index;

        abstract OptionalOneToManyIndex<Coordinate, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        @Test
        void putOrUpdate_whenOptionalIsEmpty_thenStateNotIndexed() {
            cache.add(new TestState(Id.create(1), Optional.empty()));
            assertThat(index.keySet().stream().mapToInt(index::count).sum()).isEqualTo(0);
        }

        @Test
        void putOrUpdate_whenOptionalIsPresent_thenStateIndexed() {
            TestState testState = new TestState(Id.create(1), Optional.of(Coordinate.ORIGIN));
            cache.add(testState);

            assertThat(index.stream(Coordinate.ORIGIN)).first().isEqualTo(testState);
        }

        @Test
        void snapshot_whenOptionalIsEmpty_returnsEmptySnapshot() {
            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();

            assertThat(firstSnapshot.keySet()).isEmpty();
        }

        @Test
        void snapshot_whenOptionalIsPresent_returnsSnapshotWithSingleElement() {
            TestState testState = new TestState(Id.create(1), Optional.of(Coordinate.ORIGIN));
            cache.add(testState);
            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();

            assertThat(firstSnapshot.values()).containsOnly(testState);
        }

        @Test
        void snapshot_whenNoChangesToCache_thenSameObjectReturned() {
            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();
            ImmutableMultimap<Coordinate, TestState> secondSnapshot = index.snapshot();

            assertThat(firstSnapshot).isSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexAddedTo_newObjectReturned() {
            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();

            TestState testState = new TestState(Id.create(1), Optional.of(Coordinate.ORIGIN));
            cache.add(testState);
            ImmutableMultimap<Coordinate, TestState> secondSnapshot = index.snapshot();

            assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexRemovedFrom_newObjectReturned() {
            TestState testState = new TestState(Id.create(1), Optional.of(Coordinate.ORIGIN));
            cache.add(testState);

            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();

            cache.delete(testState.getId());

            ImmutableMultimap<Coordinate, TestState> secondSnapshot = index.snapshot();
            assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
        }
    }

    interface LocationState {
        Optional<Coordinate> getLocation();
    }

    private static final class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final Optional<Coordinate> location;

        private TestState(Id<TestState> id, Optional<Coordinate> location) {
            super(id);
            this.location = location;
        }

        @Override
        public Optional<Coordinate> getLocation() {
            return location;
        }
    }
}
