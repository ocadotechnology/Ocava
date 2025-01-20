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
package com.ocadotechnology.scenario;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Disabled;

/**
 * This annotation can be used to identify tests that are currently expected to fail. When this annotation is applied
 * to a test that throws an exception during execution of its steps then the test will be marked as "passed" and vice versa.
 * This allows still valid, yet failing, tests to be highlighted so that the test/implementation can be updated as required.
 * Errors encountered outside of executing the test steps (such as unsupported combinations of check step modifiers) will cause
 * the test to fail as normal.
 *
 * A test which is failing intermittently (e.g. due to random seed instability) should be {@link Disabled} instead.
 **/
@Target(value = {ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FixRequired {
    String value();
}
