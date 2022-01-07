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

import com.ocadotechnology.id.Identified;

/**
 * An abstract class rather than an interface so that access to modification can be hidden for the implementations.
 */
public abstract class Index<C extends Identified<?>> {
    protected abstract void update(@Nullable C newObject, @Nullable C oldObject);

    protected abstract void updateAll(Iterable<Change<C>> changes);
}
