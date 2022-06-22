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

import java.util.Arrays;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.indexedcache.TestIndex.Action;

/**
 * Testing the logic within the AbstractIndex class by creating a test index
 */
public class AbstractIndexTest {
    private static final TestState OLD_STATE_1 = createObject(1, 0);
    private static final TestState NEW_STATE_1 = createObject(1, 1);
    private static final TestState OLD_STATE_2 = createObject(2, 2);
    private static final TestState NEW_STATE_3 = createObject(3, 3);
    private static final TestState OLD_STATE_4 = createObject(4, 4);
    private static final TestState NEW_STATE_4 = createObject(4, 5);
    private static final TestState OLD_STATE_5 = createObject(5, 6);
    private static final TestState NEW_STATE_5 = createObject(5, 7);

    ImmutableList<Change<TestState>> CHANGES = ImmutableList.of(
            Change.update(OLD_STATE_1, NEW_STATE_1),
            Change.delete(OLD_STATE_2),
            Change.add(NEW_STATE_3),
            Change.update(OLD_STATE_4, NEW_STATE_4),
            Change.update(OLD_STATE_5, NEW_STATE_5));

    private final TestIndex index = new TestIndex();

    @ParameterizedTest(name = "old={0} new={1}")
    @MethodSource(value = "getUpdateCases")
    void testUpdate_whenNoErrorsThrown_thenUpdateIsApplied(@CheckForNull TestState oldState, @CheckForNull TestState newState) throws IndexUpdateException {
        index.update(newState, oldState);

        assertSequence(filterNulls(oldState, newState));
    }

    private Action[] filterNulls(@CheckForNull TestState removed, @CheckForNull TestState added) {
        if (removed == null) {
            return new Action[]{Action.added(added)};
        }
        if (added == null) {
            return new Action[]{Action.removed(removed)};
        }
        return new Action[]{Action.removed(removed), Action.added(added)};
    }

    @Test
    void testUpdate_whenErrorThrownOnRemove_thenNoFurtherUpdateIsApplied() {
        index.throwWhenRemovingValue(OLD_STATE_1.getValue());
        Assertions.assertThatThrownBy(() -> index.update(NEW_STATE_1, OLD_STATE_1))
                        .isInstanceOf(IndexUpdateException.class);

        assertSequence(Action.removed(OLD_STATE_1));
    }

    @Test
    void testUpdate_whenErrorThrownOnAdd_thenRemoveIsRolledBack() {
        index.throwWhenAddingValue(NEW_STATE_1.getValue());
        Assertions.assertThatThrownBy(() -> index.update(NEW_STATE_1, OLD_STATE_1))
                .isInstanceOf(IndexUpdateException.class);

        assertSequence(
                Action.removed(OLD_STATE_1), //Healthy
                Action.added(NEW_STATE_1), //Failed
                Action.added(OLD_STATE_1)); //Rollback first
    }

    @Test
    void testUpdate_whenErrorThrownOnRollback_thenThrowsUncheckedException() {
        index.throwWhenAddingValue(NEW_STATE_1.getValue());
        index.throwWhenAddingValue(OLD_STATE_1.getValue());
        Assertions.assertThatThrownBy(() -> index.update(NEW_STATE_1, OLD_STATE_1))
                .isInstanceOf(IllegalStateException.class);

        assertSequence(
                Action.removed(OLD_STATE_1), //Healthy
                Action.added(NEW_STATE_1), //Failed
                Action.added(OLD_STATE_1)); //Rollback first - also fails
    }

    @Test
    void testUpdateAll_whenNoExceptionsThrown_thenExpectedValuesAreRemovedAndThenAdded() throws IndexUpdateException {
        index.updateAll(CHANGES);

        assertSequence(
                Action.removed(OLD_STATE_1),
                Action.removed(OLD_STATE_2),
                Action.removed(OLD_STATE_4),
                Action.removed(OLD_STATE_5),
                Action.added(NEW_STATE_1),
                Action.added(NEW_STATE_3),
                Action.added(NEW_STATE_4),
                Action.added(NEW_STATE_5)
        );
    }

    @Test
    void testUpdateAll_whenFailsOnRemove_thenRollsBackEarlierRemoves() {
        index.throwWhenRemovingValue(OLD_STATE_4.getValue());
        Assertions.assertThatThrownBy(() -> index.updateAll(CHANGES))
                .isInstanceOf(IndexUpdateException.class);

        assertSequence(
                Action.removed(OLD_STATE_1),
                Action.removed(OLD_STATE_2),

                Action.removed(OLD_STATE_4), //Failure

                Action.added(OLD_STATE_1),
                Action.added(OLD_STATE_2)
        );
    }

    @Test
    void testUpdateAll_whenFailsOnAdd_thenRollsBackEarlierAddsAndAllRemoves() {
        index.throwWhenAddingValue(NEW_STATE_4.getValue());
        Assertions.assertThatThrownBy(() -> index.updateAll(CHANGES))
                .isInstanceOf(IndexUpdateException.class);

        assertSequence(
                Action.removed(OLD_STATE_1),
                Action.removed(OLD_STATE_2),
                Action.removed(OLD_STATE_4),
                Action.removed(OLD_STATE_5),
                Action.added(NEW_STATE_1),
                Action.added(NEW_STATE_3),

                Action.added(NEW_STATE_4), //Failure

                Action.removed(NEW_STATE_1),
                Action.removed(NEW_STATE_3),
                Action.added(OLD_STATE_1),
                Action.added(OLD_STATE_2),
                Action.added(OLD_STATE_4),
                Action.added(OLD_STATE_5)
        );
    }

    @Test
    void testUpdateAll_whenFailsOnRollbackOfRemove_thenThrowsUncheckedException() {
        index.throwWhenRemovingValue(OLD_STATE_4.getValue());
        index.throwWhenAddingValue(OLD_STATE_1.getValue());
        Assertions.assertThatThrownBy(() -> index.updateAll(CHANGES))
                .isInstanceOf(IllegalStateException.class);

        assertSequence(
                Action.removed(OLD_STATE_1),
                Action.removed(OLD_STATE_2),

                Action.removed(OLD_STATE_4), //Failure
                Action.added(OLD_STATE_1) //Second failure
        );
    }

    @Test
    void testUpdateAll_whenFailsOnRollbackOfAdd_thenThrowsUncheckedException() {
        index.throwWhenAddingValue(NEW_STATE_4.getValue());
        index.throwWhenRemovingValue(NEW_STATE_1.getValue());
        Assertions.assertThatThrownBy(() -> index.updateAll(CHANGES))
                .isInstanceOf(IllegalStateException.class);

        assertSequence(
                Action.removed(OLD_STATE_1),
                Action.removed(OLD_STATE_2),
                Action.removed(OLD_STATE_4),
                Action.removed(OLD_STATE_5),
                Action.added(NEW_STATE_1),
                Action.added(NEW_STATE_3),

                Action.added(NEW_STATE_4), //Failure
                Action.removed(NEW_STATE_1) //Second failure
        );
    }

    private void assertSequence(Action... expectedActions) {
        Assertions.assertThat(index.getRecordedActions())
                .withFailMessage("Incorrect sequence of actions")
                .isEqualTo(Arrays.asList(expectedActions));
    }

    private static Stream<Arguments> getUpdateCases() {
        return Stream.of(
                Arguments.of(OLD_STATE_1, NEW_STATE_1),
                Arguments.of(null, NEW_STATE_1),
                Arguments.of(OLD_STATE_1, null)
        );
    }

    private static TestState createObject(long id, long value) {
        return new TestState(Id.create(id), false, value);
    }

}
