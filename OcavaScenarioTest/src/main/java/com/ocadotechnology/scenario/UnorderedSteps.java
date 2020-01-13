/*
 * Copyright © 2017-2019 Ocado (Ocava)
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
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

import com.google.common.collect.ImmutableList;

public class UnorderedSteps {
    private final StepCache stepCache;
    private final StepManager stepManager;
    private final boolean isFailingStep;

    private UnorderedSteps(StepManager stepManager, boolean isFailingStep) {
        this.stepManager = stepManager;
        this.stepCache = stepManager.getStepsCache();
        this.isFailingStep = isFailingStep;
    }

    public UnorderedSteps(StepManager stepManager) {
        this(stepManager, false);
    }

    public UnorderedSteps failingStep() {
        return new UnorderedSteps(stepManager, true);
    }

    private void addSimpleExecuteStep(Runnable runnable) {
        ExecuteStep step = new SimpleExecuteStep(runnable);
        if (isFailingStep) {
            stepCache.addFailingStep(step);
        }
        stepManager.add(step);
    }

    private void addExecuteStep(ExecuteStep step) {
        if (isFailingStep) {
            stepCache.addFailingStep(step);
        }
        stepManager.add(step);
    }

    /**
     * Used to remove unordered steps by name, which is particularly useful for removing a "never" step that no longer applies.
     *
     * @param names the names of steps that should be removed
     */
    public void removesUnorderedSteps(String... names) {
        addSimpleExecuteStep(() -> {
            for (String name : names) {
                stepCache.removeAndCancel(name);
            }
        });
    }

    /**
     * Like removesUnorderedSteps, except continues even if the step was never added.
     *
     * @param names the names of steps that this step will try to remove
     */
    public void removesUnorderedStepsIfPresent(String... names) {
        addSimpleExecuteStep(() -> {
            for (String name : names) {
                stepCache.removeAndCancelIfPresent(name);
            }
        });
    }

    /**
     * Used to wait for unordered steps specified by name, to fix the group specified by the names to have to have
     * happened before subsequent steps. This is not required if there are no subsequent steps (the scenario test will
     * wait for the steps anyway).
     *
     * @param names the names of steps that this step will wait for
     */
    public void waitForSteps(String... names) {
        addExecuteStep(new ExecuteStep() {
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
     *
     * @param names the names of steps that this step will try to wait for
     */
    public void waitForStepsIfPresent(String... names) {
        addExecuteStep(new ExecuteStep() {
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
     * Used to wait for any of a list of unordered steps to have happened before subsequent steps. This allows an OR of
     * unordered steps to be done e.g. for scenarios with multiple valid sequences of events.
     *
     * @param names the names of steps that this step will wait for
     *
     * @return the list of steps that have finished and therefore caused this wait step to finish
     */
    public StepFuture<List<String>> waitForAnyOfSteps(String... names) {
        MutableStepFuture<List<String>> finishedStepsFuture = new MutableStepFuture<>();

        addExecuteStep(new ExecuteStep() {
            private ImmutableList<String> waitSteps = ImmutableList.copyOf(names);
            private boolean isComplete = false;

            @Override
            protected void executeStep() {
                Assertions.assertTrue(waitSteps.stream().allMatch(stepCache::hasAddedStepWithName),
                        "Not all steps that we are waiting for have been previously added. Waiting for: "
                                + waitSteps + ", previously added: " + stepCache.getAllUnorderedStepNames());

                List<String> finishedSteps = waitSteps.stream()
                        .filter(stepCache::isUnorderedStepFinished)
                        .collect(Collectors.toList());
                if (!finishedSteps.isEmpty()) {
                    finishedStepsFuture.populate(finishedSteps);
                    waitSteps.forEach(stepCache::removeAndCancelIfPresent);
                    isComplete = true;
                }
            }

            @Override
            public boolean isFinished() {
                return isComplete;
            }

            @Override protected String info() {
                return "Waiting for any of steps " + waitSteps + " to finish; have any finished = " + isComplete;
            }
        });

        return finishedStepsFuture;
    }

    /**
     * Asserts that the steps specified by name are finished by the time this step executes.
     *
     * @param names the steps that should have already finished
     */
    public void allStepsAreAlreadyFinished(String... names) {
        addSimpleExecuteStep(() -> {
            ImmutableList<String> unfinished = Arrays.stream(names).filter(name -> !stepCache.isUnorderedStepFinished(name)).collect(ImmutableList.toImmutableList());
            Assertions.assertTrue(unfinished.isEmpty(), "Steps " + unfinished + " are not finished");
        });
    }

    /**
     * Asserts that step a is finished before step b, unless they are both finished before this step executes, in which
     * case, there is currently no way to tell. FIXME: "correct" this behaviour?
     *
     * @param a the name of the step which should finish first
     * @param b the name of the step which should finish second
     */
    public void stepAIsFinishedBeforeStepB(String a, String b) {
        String name = stepCache.getRandomUnorderedStepName();
        ExecuteStep step = new ExecuteStep() {
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
        };
        if (isFailingStep) {
            stepCache.addFailingStep(step);
        }
        stepManager.add(name, step);
    }
}
