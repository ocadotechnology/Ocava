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
package com.ocadotechnology.junit5.suite.engine.repeating;

import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * A simple TestTemplateInvocationContext for a test which is being repeated a known number of times
 */
public class RepetitionTestTemplateInvocationContext implements TestTemplateInvocationContext {

    private final int repetitionNumber;
    private final int totalRepetitions;

    public RepetitionTestTemplateInvocationContext(int repetitionNumber, int totalRepetitions) {
        this.repetitionNumber = repetitionNumber;
        this.totalRepetitions = totalRepetitions;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
        return "Repetition " + repetitionNumber + " of " + totalRepetitions;
    }
}
