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

import javax.annotation.CheckForNull;

import com.google.common.base.MoreObjects;

class CacheMismatch<C> {
    @CheckForNull final C presentObject;
    @CheckForNull final C expectedObject;

    CacheMismatch(C presentObject, C expectedObject) {
        this.presentObject = presentObject;
        this.expectedObject = expectedObject;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("presentObject", presentObject)
                .add("expectedObject", expectedObject)
                .toString();
    }
}
