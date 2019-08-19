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
import java.util.OptionalDouble;

public class SerializableOptionalDouble implements Serializable {
    private static final long serialVersionUID = 1L;
    private final OptionalDouble value;

    public SerializableOptionalDouble(OptionalDouble value) {
        this.value = value;
    }

    public static  SerializableOptionalDouble of(OptionalDouble value) {
        return new SerializableOptionalDouble(value);
    }

    public static  SerializableOptionalDouble ofNullable(Double value) {
        OptionalDouble optionalDouble = value != null ? OptionalDouble.of(value) : OptionalDouble.empty();
        return new SerializableOptionalDouble(optionalDouble);
    }

    public OptionalDouble getWrapped() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializableOptionalDouble that = (SerializableOptionalDouble) o;
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
        return new SerializableOptionalDoubleProxy(value);
    }

    private static class SerializableOptionalDoubleProxy implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Double value;

        private SerializableOptionalDoubleProxy(OptionalDouble value) {
            this.value = value.isPresent() ? value.getAsDouble() : null;
        }

        private Object readResolve() throws ObjectStreamException {
            return SerializableOptionalDouble.ofNullable(value);
        }
    }
}
