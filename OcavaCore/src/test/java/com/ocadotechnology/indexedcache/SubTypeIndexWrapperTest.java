/*
 * Copyright © 2017-2026 Ocado (Ocava)
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.id.Id;

class SubTypeIndexWrapperTest {
    private final RecordingSubTypeIndex recordingIndex = new RecordingSubTypeIndex();
    private final SubTypeIndexWrapper<TestState, ExtendedTestState> wrapper =
            new SubTypeIndexWrapper<>("subtype-index", ExtendedTestState.class, recordingIndex);

    @Test
    void update_whenBothStatesAreSubtype_delegatesToSubTypeIndex() throws IndexUpdateException {
        ExtendedTestState oldState = extendedState(1);
        ExtendedTestState newState = extendedState(1);

        wrapper.update(newState, oldState);

        assertThat(recordingIndex.singleUpdateCalls).isEqualTo(1);
        assertThat(recordingIndex.lastNewState).isSameAs(newState);
        assertThat(recordingIndex.lastOldState).isSameAs(oldState);
    }

    @Test
    void update_whenNeitherStateMatchesSubtype_doesNotDelegate() throws IndexUpdateException {
        wrapper.update(state(2), state(2));

        assertThat(recordingIndex.singleUpdateCalls).isZero();
    }

    @Test
    void update_whenNewStateIsNullAndOldStateDoesNotMatchSubtype_doesNotDelegate() throws IndexUpdateException {
        wrapper.update(null, state(3));

        assertThat(recordingIndex.singleUpdateCalls).isZero();
    }

    @Test
    void update_whenOldStateIsNullAndNewStateDoesNotMatchSubtype_doesNotDelegate() throws IndexUpdateException {
        wrapper.update(state(4), null);

        assertThat(recordingIndex.singleUpdateCalls).isZero();
    }

    @Test
    void update_whenBothStatesExistAndOnlyOneMatchesSubtype_throwsIndexUpdateException() {
        assertThatThrownBy(() -> wrapper.update(state(5), extendedState(5)))
                .isInstanceOf(IndexUpdateException.class)
                .hasMessageContaining("Inconsistent types for old and new objects");

        assertThat(recordingIndex.singleUpdateCalls).isZero();
    }

    @Test
    void updateAll_whenMixedChangesProvided_delegatesOnlySubtypeChanges() throws IndexUpdateException {
        ExtendedTestState oldExtended = extendedState(10);
        ExtendedTestState newExtended = extendedState(10);
        ExtendedTestState addedExtended = extendedState(11);

        ImmutableList<Change<TestState>> changes = ImmutableList.of(
                Change.update(oldExtended, newExtended),
                Change.update(state(12), state(12)),
                Change.add(addedExtended));

        wrapper.updateAll(changes);

        assertThat(recordingIndex.batchUpdateCalls).isEqualTo(1);
        assertThat(recordingIndex.lastBatchChanges).hasSize(2);
        assertThat(recordingIndex.lastBatchChanges.get(0).originalObject).isSameAs(oldExtended);
        assertThat(recordingIndex.lastBatchChanges.get(0).newObject).isSameAs(newExtended);
        assertThat(recordingIndex.lastBatchChanges.get(1).originalObject).isNull();
        assertThat(recordingIndex.lastBatchChanges.get(1).newObject).isSameAs(addedExtended);
    }

    @Test
    void updateAll_whenChangeHasNullAndNonSubtype_doesNotThrowAndDoesNotDelegate() throws IndexUpdateException {
        ImmutableList<Change<TestState>> changes = ImmutableList.of(Change.add(state(19)));

        wrapper.updateAll(changes);

        assertThat(recordingIndex.batchUpdateCalls).isZero();
    }

    @Test
    void updateAll_whenAnyChangeHasInconsistentSubtypeWithBothStatesPresent_throwsAndDoesNotDelegate() {
        ImmutableList<Change<TestState>> changes = ImmutableList.of(Change.update(state(20), extendedState(20)));

        assertThatThrownBy(() -> wrapper.updateAll(changes))
                .isInstanceOf(IndexUpdateException.class)
                .hasMessageContaining("Inconsistent types for old and new objects");

        assertThat(recordingIndex.batchUpdateCalls).isZero();
    }

    private static TestState state(long id) {
        return new TestState(Id.create(id), false, id);
    }

    private static ExtendedTestState extendedState(long id) {
        return new ExtendedTestState(Id.create(id), false, id);
    }

    private static class RecordingSubTypeIndex extends Index<ExtendedTestState> {
        private int singleUpdateCalls;
        private int batchUpdateCalls;
        private ExtendedTestState lastNewState;
        private ExtendedTestState lastOldState;
        private List<Change<ExtendedTestState>> lastBatchChanges = List.of();

        @Override
        protected void update(ExtendedTestState newObject, ExtendedTestState oldObject) {
            singleUpdateCalls++;
            lastNewState = newObject;
            lastOldState = oldObject;
        }

        @Override
        protected void updateAll(Iterable<Change<ExtendedTestState>> changes) {
            batchUpdateCalls++;
            lastBatchChanges = ImmutableList.copyOf(changes);
        }
    }
}

