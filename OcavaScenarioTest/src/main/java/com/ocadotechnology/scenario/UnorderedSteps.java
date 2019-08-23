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
package com.ocadotechnology.scenario;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import com.google.common.collect.ImmutableList;

public class UnorderedSteps {
    private final StepCache stepCache;
    private final StepManager stepManager;

    public UnorderedSteps(StepCache stepCache, StepManager stepManager) {
        this.stepCache = stepCache;
        this.stepManager = stepManager;
    }

    /**
     * Used to remove unordered steps by name, which is particularly useful for removing a "never" step that no longer applies.
     */
    public void removesUnorderedSteps(String... names) {
        stepManager.addExecuteStep(() -> {
            for (String name : names) {
                stepCache.removeAndCancel(name);
            }
        });
    }

    /**
     * Like removesUnorderedSteps, except continues even if the step was never added.
     */
    public void removesUnorderedStepsIfPresent(String... names) {
        stepManager.addExecuteStep(() -> {
            for (String name : names) {
                stepCache.removeAndCancelIfPresent(name);
            }
        });
    }

    /**
     * Used to wait for unordered steps specified by name, to fix the group specified by the names to have to have
     * happened before subsequent steps. This is not required if there are no subsequent steps (the scenario test will
     * wait for the steps anyway).
     */
    public void waitForSteps(String... names) {
        stepManager.add(new ExecuteStep() {
            private final List<String> waitSteps = new LinkedList<>(Arrays.asList(names));

            @Override
            protected void executeStep() {
                Assertions.assertTrue(waitSteps.stream().allMatch(stepCache::hasAddedStepWithName),
                        "Not all steps that we are waiting for have been previously added. Waiting for: "
                                        + waitSteps + ", previously added: " + stepCache.getAllUnorderedStepNames());
                waitSteps.removeIf(stepCache::isUnorderedStepFinished);
            }

            @Override
            public boolean isFinished() {
                return waitSteps.isEmpty();
            }

            @Override protected String info() {
                return waitSteps.toString();
            }
        });
    }

    /**
     * Like waitForSteps, except continues even if the step was never added.
     */
    public void waitForStepsIfPresent(String... names) {
        stepManager.add(new ExecuteStep() {
            private final List<String> waitSteps = new LinkedList<>(Arrays.asList(names));

            @Override
            protected void executeStep() {
                waitSteps.removeIf(stepCache::isUnorderedStepFinished);
            }

            @Override
            public boolean isFinished() {
                return waitSteps.isEmpty();
            }

            @Override protected String info() {
                return waitSteps.toString();
            }
        });
    }

    /**
     * Asserts that the steps specified by name are finished by the time this step executes.
     */
    public void allStepsAreAlreadyFinished(String... names) {
        stepManager.addExecuteStep(() -> {
            ImmutableList<String> unfinished = Arrays.stream(names).filter(name -> !stepCache.isUnorderedStepFinished(name)).collect(ImmutableList.toImmutableList());
            Assertions.assertTrue(unfinished.isEmpty(), "Steps " + unfinished + " are not finished");
        });
    }

    /**
     * Asserts that step a is finished before step b, unless they are both finished before this step executes, in which
     * case, there is currently no way to tell. FIXME: "correct" this behaviour?
     */
    public void stepAIsFinishedBeforeStepB(String a, String b) {
        String name = stepCache.getRandomUnorderedStepName();
        stepManager.add(name, new ExecuteStep() {
            private boolean finished = false;

            @Override
            protected void executeStep() {
                boolean isAFinished = stepCache.isUnorderedStepFinished(a);
                boolean isBFinished = stepCache.isUnorderedStepFinished(b);
                if (isAFinished && isBFinished) {
                    finished = true;
                } else if (isAFinished || isBFinished) {
                    Assertions.assertTrue(!isBFinished, "Step " + b + " has been finished before step " + a);
                }
            }

            @Override
            public boolean isFinished() {
                return finished;
            }
        });
    }
}
