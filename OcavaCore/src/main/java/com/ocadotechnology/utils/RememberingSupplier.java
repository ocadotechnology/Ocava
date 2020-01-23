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
package com.ocadotechnology.utils;

import java.util.function.Supplier;

import com.google.common.base.Preconditions;

public class RememberingSupplier<T> implements Supplier<T> {
    private final Supplier<T> supplier;
    private T value;

    public RememberingSupplier(Supplier<T> supplier) {
        this.supplier = Preconditions.checkNotNull(supplier, "RememberingSupplier can't be initialised with a null supplier");
    }

    public RememberingSupplier(T value) {
        supplier = null;
        this.value = Preconditions.checkNotNull(value, "RememberingSupplier can't be initialised with a null value");
    }

    @Override
    public T get() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }
}
