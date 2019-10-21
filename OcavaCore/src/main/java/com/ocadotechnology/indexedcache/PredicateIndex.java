/*
 * Copyright © 2017-2019 Ocado (Ocava)
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
import java.util.stream.Stream;

import com.ocadotechnology.id.Identified;

public final class PredicateIndex<C extends Identified<?>> extends AbstractIndex<C> {
    private final OneToManyIndex<Boolean, C> index;

    PredicateIndex(Predicate<? super C> predicate) {
        this.index = new OneToManyIndex<>(predicate::test);
    }

    public Stream<C> stream() {
        return index.stream(true);
    }

    public int count() {
        return index.count(true);
    }

    public Stream<C> streamWhereNot() {
        return index.stream(false);
    }

    public int countWhereNot() {
        return index.count(false);
    }

    @Override
    protected void add(C object) {
        index.add(object);
    }

    @Override
    protected void remove(C object) {
        index.remove(object);
    }

    public boolean isEmpty() {
        return index.count(true) == 0;
    }
}
