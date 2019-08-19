/*
 * Copyright © 2017 Ocado (Ocava)
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
package com.ocadotechnology.serialization;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class SerializableOptional<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Optional<T> value;

    public SerializableOptional(Optional<T> value) {
        this.value = value;
    }

    public static <T extends Serializable> SerializableOptional<T> of(Optional<T> value) {
        return new SerializableOptional<>(value);
    }

    public Optional<T> getWrapped() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SerializableOptional<?> that = (SerializableOptional<?>) o;

        return !(value != null ? !value.equals(that.value) : that.value != null);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    private Object writeReplace() throws ObjectStreamException {
        return new SerializableOptionalProxy<>(value);
    }

    private static class SerializableOptionalProxy<T extends Serializable> implements Serializable {
        private static final long serialVersionUID = 1L;
        private final T value;

        private SerializableOptionalProxy(Optional<T> value) {
            this.value = value.orElse(null);
        }

        private Object readResolve() throws ObjectStreamException {
            return new SerializableOptional<>(Optional.ofNullable(this.value));
        }
    }
}
