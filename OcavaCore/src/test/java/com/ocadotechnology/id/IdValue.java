/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.converter.ConvertWith;

/**
 * Annotation for use in jUnit5 parameterised tests, to tag parameters of type {@link Id} in cases where the parameter
 * value is a String representation of the underlying ID.
 * 
 * For example:
 * <pre>
 *{@literal @}ParameterizedTest(name = "user={0}, account={1}")
 *{@literal @}CsvSource({
 *     "23, 15145681",
 *     "99, 89165213",
 *     " 0, 10193113",
 * })
 *{@code void testAddingUserAccount(@IdValue Id<User> userId, @IdValue Id<Account> accountId)} {
 *     ...
 * }
 * </pre>
 * 
 * @see <a href="https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests-argument-conversion-explicit">junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests-argument-conversion-explicit</a>
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@ConvertWith(IdConverter.class)
public @interface IdValue {
}
