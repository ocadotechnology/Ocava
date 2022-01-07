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
package com.ocadotechnology.id;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Abstract base class for instances of {@link Identified} where equality is determined solely on the ID.
 * 
 * @param <T> Type of objects with which the ID is associated.
 * @param <I> Type of the ID itself.
 */
@Immutable
@ParametersAreNonnullByDefault
abstract class SimpleIdentified<T, I extends Identity<T>> implements Identified<T> {
    
    private final I id;

    SimpleIdentified(I id) {
        this.id = id;
    }

    @Override
    public final I getId() {
        return id;
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SimpleIdentified<?, ?> that = (SimpleIdentified<?, ?>) o;
        return id.equals(that.id);
    }
}
