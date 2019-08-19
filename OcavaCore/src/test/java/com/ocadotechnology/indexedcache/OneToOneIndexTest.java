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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("A OneToOneIndex")
class OneToOneIndexTest {
    
    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        OneToOneIndex<Coordinate, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOneToOneIndex(TestState::getLocation);
        }
    }
    
    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        OneToOneIndex<Coordinate, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOneToOneIndex() require a type of
            // Function<TestState, Coordinate> instead of Function<? super TestState, Coordinate>, due to automatic type
            // coercion of the lambda.
            Function<LocationState, Coordinate> indexFunction = LocationState::getLocation;
            return cache.addOneToOneIndex(indexFunction);
        }
    }
    
    private abstract static class IndexTests {

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private OneToOneIndex<Coordinate, TestState> index;

        abstract OneToOneIndex<Coordinate, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);
        
        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        @Test
        void putOrUpdate_whenFunctionReturnsNull_thenExceptionThrownOnAdd() {
            TestState stateOne = new TestState(Id.create(1), null);

            assertThatThrownBy(() -> cache.add(stateOne))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void putOrUpdate_whenMultipleTestStatesMapToTheSameCoordinate_thenExceptionThrownOnAdd() {
            TestState stateOne = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState stateTwo = new TestState(Id.create(2), Coordinate.create(0, 0));

            assertThatCode(() -> cache.add(stateOne)).doesNotThrowAnyException();
            assertThatThrownBy(() -> cache.add(stateTwo)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void putOrUpdateAll_whenMultipleTestStatesMapToTheSameCoordinate_thenExceptionThrownOnAdd() {
            TestState stateOne = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState stateTwo = new TestState(Id.create(2), Coordinate.create(0, 0));

            assertThatThrownBy(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void putOrUpdateAll_whenTestStateMapsToCoordinateMappedToBySomeThingElse_thenExceptionThrownOnAdd() {
            TestState stateOne = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState stateTwo = new TestState(Id.create(2), Coordinate.create(0, 1));
            TestState stateThree = new TestState(Id.create(3), Coordinate.create(0, 0));

            assertThatCode(() -> cache.add(stateOne)).doesNotThrowAnyException();
            assertThatThrownBy(() -> cache.addAll(ImmutableSet.of(stateTwo, stateThree)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void putOrUpdateAll_whenLocationsAreSwappedTheyAreSwappedInIndex() {
            TestState stateOne = new TestState(Id.create(1), Coordinate.create(0, 1));
            TestState stateTwo = new TestState(Id.create(2), Coordinate.create(1, 0));

            cache.addAll(ImmutableSet.of(stateOne, stateTwo));

            Change<TestState> updateOne = Change.update(stateOne, new TestState(Id.create(1), Coordinate.create(1, 0)));
            Change<TestState> updateTwo = Change.update(stateTwo, new TestState(Id.create(2), Coordinate.create(0, 1)));

            cache.updateAll(ImmutableSet.of(updateOne, updateTwo));

            assertThat(index.get(Coordinate.create(1, 0))).isEqualTo(stateOne);
            assertThat(index.get(Coordinate.create(0, 1))).isEqualTo(stateTwo);
        }
    }

    private interface LocationState {
        Coordinate getLocation();
    }
    
    private static class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final Coordinate location;

        private TestState(Id<TestState> id, Coordinate location) {
            super(id);
            this.location = location;
        }

        @Override
        public Coordinate getLocation() {
            return location;
        }
    }
}
