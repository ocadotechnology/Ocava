/*
 * Copyright © 2017-2021 Ocado (Ocava)
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

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;

/**
 * For use with jUnit5 parameterised tests, to enable String-based IDs to be passed into the test method as an {@link
 * Id} instance. The conversion logic will be called automatically by the jUnit5 test runner for parameters that are
 * annotated with {@link IdValue}.
 * 
 * @param <T> The type of objects for which this ID applies.
 */
class IdConverter<T> implements ArgumentConverter {
    @Override
    public Id<T> convert(Object source, ParameterContext context) throws ArgumentConversionException {
        try {
            return Id.create(Long.parseLong((String) source));
        } catch (NumberFormatException e) {
            throw new ArgumentConversionException("Cannot convert to Id", e);
        }
    }
}
