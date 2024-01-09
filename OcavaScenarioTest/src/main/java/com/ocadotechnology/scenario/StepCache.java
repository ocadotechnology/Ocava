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
package com.ocadotechnology.scenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ocadotechnology.event.scheduling.Cancelable;

public class StepCache extends Cleanable {
    private static final Predicate<Throwable> EXCEPTION_CHECKER_DEFAULT = t -> false;
    private LinkedList<Executable> orderedSteps = new LinkedList<>();
    private Executable lastStep;
    private Multimap<String, Executable> unorderedSteps = LinkedHashMultimap.create();
    private final Set<String> allUnorderedStepNames = new HashSet<>();
    private int stepCounter = 0;
    private List<Executable> finalSteps = new ArrayList<>();
    private Predicate<Throwable> exceptionChecker = EXCEPTION_CHECKER_DEFAULT;
    private List<Executable> failingSteps = new ArrayList<>();

    public int getNextStepCounter() {
        return stepCounter++;
    }

    public void addCheckStep(ExceptionCheckStep testStep) {
        addOrdered(testStep);
        exceptionChecker = testStep::checkThrowable;
    }

    public Predicate<Throwable> getExceptionChecker() {
        return exceptionChecker;
    }

    public void addOrdered(Executable testStep) {
        validateExceptionAsLastStep(testStep);
        if (isMergeable(testStep)) {
            lastStep.merge(testStep);
        } else {
            orderedSteps.add(testStep);
        }
        lastStep = testStep;
    }

    public void addOrdered(int idx, Executable testStep) {
        validateExceptionAsLastStep(testStep);
        if (idx == orderedSteps.size()) {
            addOrdered(testStep);
        } else {
            orderedSteps.add(idx, testStep);
        }
    }

    private void validateExceptionAsLastStep(Executable testStep) {
        Preconditions.checkState(exceptionChecker == EXCEPTION_CHECKER_DEFAULT, "You can not add another steps after Exception step. Invalid step [%s] ", testStep);
    }

    public void clearOrderedSteps() {
        orderedSteps.clear();
        lastStep = null;
    }

    public void clearUnorderedSteps() {
        unorderedSteps.clear();
        allUnorderedStepNames.clear();
    }

    public ImmutableList<Executable> getOrderedStepsView() {
        return ImmutableList.copyOf(orderedSteps);
    }

    public void addFinalStep(Executable testStep) {
        finalSteps.add(testStep);
    }

    public List<Executable> getFinalSteps() {
        return finalSteps;
    }

    public Executable removeLastStep() {
        LinkedList<Executable> list = orderedSteps;
        Executable removedSteps = list.removeLast();
        lastStep = list.getLast();
        return removedSteps;
    }

    private boolean isMergeable(Executable testStep) {
        return testStep.isMergeable() && lastStep != null && lastStep.isMergeable();
    }

    public boolean isUnorderedStepFinished(String name) {
        return !unorderedSteps.containsKey(name);
    }

    public String getRandomUnorderedStepName() {
        String name = String.valueOf(System.nanoTime());
        while (unorderedSteps.containsKey(name)) {
            name = String.valueOf(System.nanoTime());
        }
        return name;
    }

    public void addUnordered(String name, Executable testStep) {
        allUnorderedStepNames.add(name);
        unorderedSteps.put(name, testStep);
    }

    public ImmutableSet<String> getAllUnorderedStepNames() {
        return ImmutableSet.copyOf(allUnorderedStepNames);
    }

    public boolean hasAddedStepWithName(String name) {
        return allUnorderedStepNames.contains(name);
    }

    public void removeAndCancel(String name) {
        Preconditions.checkState(unorderedSteps.containsKey(name),
                "Tried to remove unordered steps with name '%s', but didn't find one. unorderedSteps: %s", name, unorderedSteps);
        removeAndCancelIfPresent(name);
    }

    public void removeAndCancelIfPresent(String name) {
        unorderedSteps.removeAll(name).stream().filter(step -> step instanceof Cancelable).forEach(step -> ((Cancelable) step).cancel());
    }

    public Collection<Executable> getUnorderedSteps() {
        return unorderedSteps.values();
    }

    public boolean isFinished() {
        for (Executable unorderedStep : unorderedSteps.values()) {
            if (unorderedStep.isRequired() && !unorderedStep.isFinished()) {
                return false;
            }
        }
        return orderedSteps.isEmpty();
    }

    public Executable getUnfinishedUnorderedStep() {
        for (Executable unorderedStep : unorderedSteps.values()) {
            if (unorderedStep.isRequired() && !unorderedStep.isFinished()) {
                return unorderedStep;
            }
        }
        return null;
    }

    @Override
    public void clean() {
        orderedSteps = new LinkedList<>();
        unorderedSteps = LinkedHashMultimap.create();
        allUnorderedStepNames.clear();
        finalSteps = new ArrayList<>();
        stepCounter = 0;
        exceptionChecker = EXCEPTION_CHECKER_DEFAULT;
    }

    public Executable getNextStep() {
        return orderedSteps.poll();
    }

    public Executable peekNextStep() {
        return orderedSteps.peek();
    }

    public boolean hasSteps() {
        return !orderedSteps.isEmpty() || !unorderedSteps.isEmpty() || !finalSteps.isEmpty();
    }

    public List<Executable> getFailingSteps() {
        return failingSteps;
    }

    public void addFailingStep(Executable failingStep) {
        this.failingSteps.add(failingStep);
    }

    /**
     * Unordered steps create new CheckSteps when the scenario is running, the original check steps are removed using
     * this method to avoid incorrectly reporting which steps are marked as failingSteps. see: StepManager.addUnorderedStepOnExecutionOfStep
     */
    void removeFailingStep(Executable step) {
        this.failingSteps.remove(step);
    }
}
