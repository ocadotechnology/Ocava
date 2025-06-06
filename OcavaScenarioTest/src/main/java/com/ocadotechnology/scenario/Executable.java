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

public interface Executable {
    void setStepOrder(int stepOrder);

    /**
     * Run this step
     */
    void executeAndLog();

    /**
     * @return true if this step is completed
     */
    boolean isFinished();

    /**
     * @return true if it is necessary for this step to be {@link Executable#isFinished} for the test to have completed
     * successfully
     */
    boolean isRequired();
    boolean isMergeable();
    void merge(Executable step);

    /**
     * Inform the step that it is now the active step in the test execution.
     */
    void setActive();
}
