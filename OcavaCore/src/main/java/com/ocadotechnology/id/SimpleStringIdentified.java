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
package com.ocadotechnology.id;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Abstract parent class for instances of {@link Identified} where equality is determined solely on the ID.
 * 
 * @param <T> Type of objects with which the ID is associated.
 */
@Immutable
@ParametersAreNonnullByDefault
public abstract class SimpleStringIdentified<T> extends SimpleIdentified<T, StringId<T>> {
    protected SimpleStringIdentified(StringId<T> id) {
        super(id);
    }
}
