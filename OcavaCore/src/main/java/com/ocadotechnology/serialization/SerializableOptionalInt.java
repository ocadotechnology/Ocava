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
import java.util.OptionalInt;

public class SerializableOptionalInt implements Serializable {
    private static final long serialVersionUID = 1L;

    private final OptionalInt value;

    public SerializableOptionalInt(OptionalInt value) {
        this.value = value;
    }

    public static  SerializableOptionalInt of(OptionalInt value) {
        return new SerializableOptionalInt(value);
    }

    public static SerializableOptionalInt ofNullable(Integer value) {
        OptionalInt optionalInt = value != null ? OptionalInt.of(value) : OptionalInt.empty();
        return new SerializableOptionalInt(optionalInt);
    }

    public OptionalInt getWrapped() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializableOptionalInt that = (SerializableOptionalInt) o;
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
        return new SerializableOptionalIntProxy(value);
    }

    private static class SerializableOptionalIntProxy implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Integer value;

        private SerializableOptionalIntProxy(OptionalInt value) {
            this.value = value.isPresent() ? value.getAsInt() : null;
        }

        private Object readResolve() throws ObjectStreamException {
            return SerializableOptionalInt.ofNullable(value);
        }
    }

}
