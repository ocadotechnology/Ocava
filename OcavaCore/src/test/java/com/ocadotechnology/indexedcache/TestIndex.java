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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;

@ParametersAreNonnullByDefault
class TestIndex extends AbstractIndex<TestState> {
    private final List<Long> valuesToFailAdd = new ArrayList<>();
    private final List<Long> valuesToFailRemove = new ArrayList<>();

    private final List<Action> actions = new ArrayList<>();

    public TestIndex() {
        this(null);
    }

    public TestIndex(@CheckForNull String name) {
        super(name);
    }

    @Override
    protected void remove(TestState object) throws IndexUpdateException {
        actions.add(Action.removed(object));
        if (valuesToFailRemove.contains(object.getValue())) {
            throw new IndexUpdateException(
                    getName().orElse(TestIndex.class.getSimpleName()),
                    "Error updating " + formattedName + ": Test failure");
        }
    }

    @Override
    protected void add(TestState object) throws IndexUpdateException {
        actions.add(Action.added(object));
        if (valuesToFailAdd.contains(object.getValue())) {
            throw new IndexUpdateException(
                    getName().orElse(TestIndex.class.getSimpleName()),
                    "Error updating " + formattedName + ": Test failure");
        }
    }

    void throwWhenRemovingValue(long value) {
        this.valuesToFailRemove.add(value);
    }

    void throwWhenAddingValue(long value) {
        this.valuesToFailAdd.add(value);
    }

    List<Action> getRecordedActions() {
        return new ArrayList<>(actions);
    }

    static class Action {
        enum Direction {
            ADDED, REMOVED
        }

        private final TestState state;
        private final Action.Direction direction;

        public Action(TestState state, Action.Direction direction) {
            this.state = state;
            this.direction = direction;
        }

        public static Action removed(TestState state) {
            return new Action(state, Action.Direction.REMOVED);
        }

        public static Action added(TestState state) {
            return new Action(state, Action.Direction.ADDED);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Action action = (Action) o;
            return state == action.state
                    && direction == action.direction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, direction);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("state", state)
                    .add("direction", direction)
                    .toString();
        }
    }
}
