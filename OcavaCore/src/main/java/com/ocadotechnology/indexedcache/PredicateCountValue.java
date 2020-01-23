/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

import java.util.function.Predicate;

import com.ocadotechnology.id.Identified;

public final class PredicateCountValue<C extends Identified<?>> extends AbstractIndex<C> {
    private final Predicate<? super C> predicate;
    private long current;

    PredicateCountValue(Predicate<? super C> predicate) {
        this.predicate = predicate;
        this.current = 0;
    }

    public long getValue() {
        return current;
    }

    @Override
    protected void add(C object) {
        if (predicate.test(object)) {
            ++current;
        }
    }

    @Override
    protected void remove(C object) {
        if (predicate.test(object)) {
            --current;
        }
    }
}
