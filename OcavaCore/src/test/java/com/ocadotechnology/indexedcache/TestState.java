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

import com.google.common.base.MoreObjects;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

class TestState extends SimpleLongIdentified<TestState> implements TestThing {
    private final boolean isSomething;
    private final long longProperty;

    TestState(Id<TestState> id, boolean isSomething, long longProperty) {
        super(id);
        this.isSomething = isSomething;
        this.longProperty = longProperty;
    }

    @Override
    public boolean isSomething() {
        return isSomething;
    }

    @Override
    public long getValue() {
        return longProperty;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", getId())
                .add("isSomething", isSomething)
                .add("longProperty", longProperty)
                .toString();
    }
}
