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
package com.ocadotechnology.id;

class SimpleStringIdentifiedTest extends SimpleIdentifiedTest<StringId<Object>, SimpleStringIdentified<Object>> {

    SimpleStringIdentifiedTest() {
        super(StringId.create("Thing"));
    }

    @Override
    SimpleStringIdentified<Object> buildTestInstance(StringId<Object> id) {
        return new TestStringIdentified(id);
    }

    @Override
    StringId<Object> getDifferentId(StringId<Object> id) {
        return StringId.create(id.id + "_X");
    }

    private static final class TestStringIdentified extends SimpleStringIdentified<Object> {
        TestStringIdentified(StringId<Object> id) {
            super(id);
        }
    }
}
