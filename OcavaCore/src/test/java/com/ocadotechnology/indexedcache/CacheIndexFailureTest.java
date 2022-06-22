/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.validation.Failer;

public class CacheIndexFailureTest {
    private enum Method {
        ADD, DELETE, UPDATE, ADD_ALL, DELETE_ALL, UPDATE_ALL
    }

    private static final TestState OLD_STATE_1 = new TestState(Id.create(1), false,  0);
    private static final TestState NEW_STATE_1 = new TestState(Id.create(1), true,  1);
    private static final TestState OLD_STATE_2 = new TestState(Id.create(2), true,  2);
    private static final TestState NEW_STATE_3 = new TestState(Id.create(3), true,  3);
    private static final TestState OLD_STATE_4 = new TestState(Id.create(4), false,  4);
    private static final TestState NEW_STATE_4 = new TestState(Id.create(4), true,  5);
    private static final TestState OLD_STATE_5 = new TestState(Id.create(5), false,  6);
    private static final TestState NEW_STATE_5 = new TestState(Id.create(5), true,  7);
    private static final TestState NEW_STATE_6 = new TestState(Id.create(6), true,  8);

    private final TestObjectStore objectStore = new TestObjectStore();
    private final IndexedImmutableObjectCache<TestState, TestState> cache = new IndexedImmutableObjectCache<>(objectStore);
    private final OneToOneIndex<Long, TestState> valueIndex = cache.addOneToOneIndex(TestState::getValue);
    private final PredicateCountValue<TestState> predicateCount = cache.addPredicateCount(TestState::isSomething);
    private final TestIndex testIndex1 = cache.registerCustomIndex(new TestIndex());
    private final TestIndex testIndex2 = cache.registerCustomIndex(new TestIndex());

    @BeforeEach
    void setup() {
        cache.add(OLD_STATE_1);
        cache.add(OLD_STATE_2);
        cache.add(OLD_STATE_4);
        cache.add(OLD_STATE_5);

        setIndexToFailAllUpdates(testIndex2); //Set the second one to fail so the first will attempt rollback
        validateState(); //Just to be sure
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void testMethod_whenIndexFails_thenRollsBackAndThrowsCacheUpdateException(Method method) {
        Assertions.assertThatThrownBy(() -> performAction(method))
                .isInstanceOf(CacheUpdateException.class)
                .hasCauseInstanceOf(IndexUpdateException.class);
        validateState();
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void testMethod_whenIndexFailsToRollback_thenThrowsIllegalStateException(Method method) {
        setIndexToFailAllRollback(testIndex1);
        Assertions.assertThatThrownBy(() -> performAction(method))
                .isInstanceOf(IllegalStateException.class);
        //Rollback is not guaranteed
    }

    @ParameterizedTest
    @EnumSource(Method.class)
    void testMethod_whenObjectStoreFailsToRollback_thenThrowsIllegalStateException(Method method) {
        objectStore.failRollback();
        Assertions.assertThatThrownBy(() -> performAction(method))
                .isInstanceOf(IllegalStateException.class);
        //Rollback is not guaranteed
    }

    private void performAction(Method method) {
        switch (method) {
            case ADD:
                cache.add(NEW_STATE_3);
                break;
            case DELETE:
                cache.delete(OLD_STATE_2.getId());
                break;
            case UPDATE:
                cache.update(OLD_STATE_1, NEW_STATE_1);
                break;
            case ADD_ALL:
                cache.addAll(ImmutableList.of(NEW_STATE_3, NEW_STATE_6));
                break;
            case DELETE_ALL:
                cache.deleteAll(ImmutableList.of(OLD_STATE_1.getId(), OLD_STATE_2.getId()));
                break;
            case UPDATE_ALL:
                cache.updateAll(ImmutableList.of(
                        Change.update(OLD_STATE_1, NEW_STATE_1),
                        Change.delete(OLD_STATE_2),
                        Change.add(NEW_STATE_3),
                        Change.update(OLD_STATE_4, NEW_STATE_4),
                        Change.update(OLD_STATE_5, NEW_STATE_5),
                        Change.add(NEW_STATE_6)
                ));
                break;
            default:
                throw Failer.fail("Method " + method + " not supported");
        }
    }

    void validateState() {
        Assertions.assertThat(cache.get(Id.create(1))).isSameAs(OLD_STATE_1);
        Assertions.assertThat(cache.get(Id.create(2))).isSameAs(OLD_STATE_2);
        Assertions.assertThat(cache.containsId(Id.create(3))).isFalse();
        Assertions.assertThat(cache.get(Id.create(4))).isSameAs(OLD_STATE_4);
        Assertions.assertThat(cache.get(Id.create(5))).isSameAs(OLD_STATE_5);

        Assertions.assertThat(valueIndex.streamKeySet()).containsExactlyInAnyOrder(0L, 2L, 4L, 6L);
        Assertions.assertThat(valueIndex.get(0L)).isSameAs(OLD_STATE_1);
        Assertions.assertThat(valueIndex.get(2L)).isSameAs(OLD_STATE_2);
        Assertions.assertThat(valueIndex.get(4L)).isSameAs(OLD_STATE_4);
        Assertions.assertThat(valueIndex.get(6L)).isSameAs(OLD_STATE_5);

        Assertions.assertThat(predicateCount.getValue()).isEqualTo(1);
    }

    /**
     * throw if any of the old values are removed or if any of the new values are added
     */
    private void setIndexToFailAllUpdates(TestIndex testIndex) {
        testIndex.throwWhenRemovingValue(OLD_STATE_1.getValue());
        testIndex.throwWhenRemovingValue(OLD_STATE_2.getValue());
        testIndex.throwWhenRemovingValue(OLD_STATE_4.getValue());
        testIndex.throwWhenRemovingValue(OLD_STATE_5.getValue());

        testIndex.throwWhenAddingValue(NEW_STATE_1.getValue());
        testIndex.throwWhenAddingValue(NEW_STATE_3.getValue());
        testIndex.throwWhenAddingValue(NEW_STATE_4.getValue());
        testIndex.throwWhenAddingValue(NEW_STATE_5.getValue());
        testIndex.throwWhenAddingValue(NEW_STATE_6.getValue());
    }

    /**
     * throw if any of the old values are re-added or if any of the new values are removed
     */
    private void setIndexToFailAllRollback(TestIndex testIndex) {
        testIndex.throwWhenAddingValue(OLD_STATE_1.getValue());
        testIndex.throwWhenAddingValue(OLD_STATE_2.getValue());
        testIndex.throwWhenAddingValue(OLD_STATE_4.getValue());
        testIndex.throwWhenAddingValue(OLD_STATE_5.getValue());

        testIndex.throwWhenRemovingValue(NEW_STATE_1.getValue());
        testIndex.throwWhenRemovingValue(NEW_STATE_3.getValue());
        testIndex.throwWhenRemovingValue(NEW_STATE_4.getValue());
        testIndex.throwWhenRemovingValue(NEW_STATE_5.getValue());
        testIndex.throwWhenRemovingValue(NEW_STATE_6.getValue());
    }

    private static class TestObjectStore extends HashMapObjectStore<TestState, TestState> {
        private boolean failingRollback;

        @Override
        public void update(@Nullable TestState expectedObject, @Nullable TestState newObject) throws CacheUpdateException {
            if (failingRollback && isRollback(expectedObject, newObject)) {
                throw new CacheUpdateException("Test exception");
            }
            super.update(expectedObject, newObject);
        }

        @Override
        public void updateAll(ImmutableCollection<Change<TestState>> changes) throws CacheUpdateException {
            if (failingRollback && changes.stream().anyMatch(TestObjectStore::isRollback)) {
                throw new CacheUpdateException("Test exception");
            }
            super.updateAll(changes);
        }

        private static boolean isRollback(Change<TestState> change) {
            return isRollback(change.originalObject, change.newObject);
        }

        /**
         * returns true if we are removing any of the new states or re-adding any of the old
         */
        private static boolean isRollback(TestState oldState, TestState newState) {
            return OLD_STATE_1 == newState
                    || NEW_STATE_1 == oldState
                    || OLD_STATE_2 == newState
                    || NEW_STATE_3 == oldState
                    || OLD_STATE_4 == newState
                    || NEW_STATE_4 == oldState
                    || OLD_STATE_5 == newState
                    || NEW_STATE_5 == oldState
                    || NEW_STATE_6 == oldState;
        }

        public void failRollback() {
            failingRollback = true;
        }
    }
}
