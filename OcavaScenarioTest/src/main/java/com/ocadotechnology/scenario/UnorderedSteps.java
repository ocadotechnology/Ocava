/*
 * Copyright © 2017-2022 Ocado (Ocava)
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.ocadotechnology.simulation.Simulation;

public class UnorderedSteps<S extends Simulation> {
    private final StepCache stepCache;
    private final StepManager<S> stepManager;
    private final boolean isFailingStep;

    private UnorderedSteps(StepManager<S> stepManager, boolean isFailingStep) {
        this.stepManager = stepManager;
        this.stepCache = stepManager.getStepsCache();
        this.isFailingStep = isFailingStep;
    }

    public UnorderedSteps(StepManager<S> stepManager) {
        this(stepManager, false);
    }

    public UnorderedSteps<S> failingStep() {
        return new UnorderedSteps<>(stepManager, true);
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
     * Used to remove an unordered step by name. Useful for removing a "never" step that no longer applies.
     *
     * @param stepName the name of step to remove
     */
    public void removesUnorderedSteps(String stepName) {
        removeSteps(stepName);
    }

    /**
     * Used to remove more than one unordered step by name. Useful for removing a "never" step that no longer applies.
     *
     * @param stepName the name of step to remove
     * @param otherStepNames the names of additional steps to remove
     */
    public void removesUnorderedSteps(String stepName, String... otherStepNames) {
        String[] names = ArrayUtils.insert(0, otherStepNames, stepName);

        removeSteps(names);
    }

    private void removeSteps(String... names) {
        Preconditions.checkState(names.length > 0, "names length must be greater than zero");
        addSimpleExecuteStep(() -> Stream.of(names).forEach(stepCache::removeAndCancel));
    }

    /**
     * Like {@link #removesUnorderedSteps}, except continues even if the step was never added.
     *
     * @param stepName the name of step to try remove
     */
    public void removesUnorderedStepsIfPresent(String stepName) {
        removeStepsIfPresent(stepName);
    }

    /**
     * Like {@link #removesUnorderedSteps}, except continues even if the step was never added.
     *
     * @param stepName the name of step to try remove
     * @param otherStepNames the names of additional steps to remove
     */
    public void removesUnorderedStepsIfPresent(String stepName, String... otherStepNames) {
        String[] names = ArrayUtils.insert(0, otherStepNames, stepName);

        removeStepsIfPresent(names);
    }

    private void removeStepsIfPresent(String... names) {
        Preconditions.checkState(names.length > 0, "names length must be greater than zero");
        addSimpleExecuteStep(() -> Stream.of(names).forEach(stepCache::removeAndCancelIfPresent));
    }

    /**
     * Used to wait for a single unordered step specified by name, to fix the group specified by the name to have
     * happened before subsequent steps. This is not required if there are no subsequent steps (the scenario test will
     * wait for the step anyway).
     *
     * @param stepName the name of step to wait for
     */
    public void waitForSteps(String stepName) {
        waitForAll(stepName);
    }

    /**
     * Used to wait for unordered steps specified by name, to fix the group specified by the names to have to have
     * happened before subsequent steps. This is not required if there are no subsequent steps (the scenario test will
     * wait for the steps anyway).
     *
     * @param stepName the name of step to wait for
     * @param otherStepNames the names of the additional steps to wait for
     */
    public void waitForSteps(String stepName, String... otherStepNames) {
        String[] names = ArrayUtils.insert(0, otherStepNames, stepName);

        waitForAll(names);
    }

    /**
     * Used to wait for unordered steps specified by name, to fix the group specified by the names to have to have
     * happened before subsequent steps. This is not required if there are no subsequent steps (the scenario test will
     * wait for the steps anyway).
     *
     * @param stepNames the names of steps to wait for, must not be empty
     */
    public void waitForSteps(Set<String> stepNames) {
        waitForAll(stepNames.toArray(new String[stepNames.size()]));
    }

    private void waitForAll(String... names) {
        Preconditions.checkState(names.length > 0, "names length must be greater than zero");

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
     * Like {@link #waitForSteps}, except continues even if the step was never added.
     *
     * @param stepName the name of step to try to wait for
     */
    public void waitForStepsIfPresent(String stepName) {
        waitForAllIfPresent(stepName);
    }

    /**
     * Like {@link #waitForSteps}, except continues even if the step was never added.
     *
     * @param stepName the name of step to try to wait for
     * @param otherStepNames the names of additional steps to try to wait for
     */
    public void waitForStepsIfPresent(String stepName, String... otherStepNames) {
        String[] names = ArrayUtils.insert(0, otherStepNames, stepName);

        waitForAllIfPresent(names);
    }

    private void waitForAllIfPresent(String... names) {
        Preconditions.checkState(names.length > 0, "names length must be greater than zero");

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
     * @param a name of a step
     * @param b name of another step
     *
     * @return the list of steps that have finished and therefore caused this wait step to finish
     */
    public StepFuture<List<String>> waitForAnyOfSteps(String a, String b) {
        return waitForAny(a, b);
    }

    /**
     * Used to wait for any of a list of unordered steps to have happened before subsequent steps. This allows an OR of
     * unordered steps to be done e.g. for scenarios with multiple valid sequences of events.
     *
     * @param a name of a step
     * @param b name of another step
     * @param otherStepNames the names of any additional optional steps to include, optional
     *
     * @return the list of steps that have finished and therefore caused this wait step to finish
     */
    public StepFuture<List<String>> waitForAnyOfSteps(String a, String b, String... otherStepNames) {
        String[] names = ArrayUtils.insert(0, otherStepNames, a, b);

        return waitForAny(names);
    }

    private StepFuture<List<String>> waitForAny(String... names) {
        Preconditions.checkState(names.length >= 2, "names length should be at least two");

        MutableStepFuture<List<String>> finishedStepsFuture = new MutableStepFuture<>();

        addExecuteStep(new ExecuteStep() {
            private List<String> waitSteps = ImmutableList.copyOf(names);
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
     * @param stepName the name of step to check
     */
    public void allStepsAreAlreadyFinished(String stepName) {
        allStepsFinished(stepName);
    }

    /**
     * Asserts that the steps specified by name are finished by the time this step executes.
     *
     * @param stepName the name of step to check
     * @param otherStepNames the names of other steps to check
     */
    public void allStepsAreAlreadyFinished(String stepName, String... otherStepNames) {
        String[] names = ArrayUtils.insert(0, otherStepNames, stepName);

        allStepsFinished(names);
    }

    private void allStepsFinished(String... names) {
        Preconditions.checkState(names.length > 0, "names length must be greater than zero");

        addSimpleExecuteStep(() -> {
            ImmutableList<String> unfinished = Stream.of(names).filter(name -> !stepCache.isUnorderedStepFinished(name)).collect(ImmutableList.toImmutableList());
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
                    Assertions.assertFalse(isBFinished, "Step " + b + " has been finished before step " + a);
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
