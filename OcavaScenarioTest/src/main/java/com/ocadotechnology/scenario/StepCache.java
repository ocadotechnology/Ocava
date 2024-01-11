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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.ocadotechnology.event.scheduling.Cancelable;

public class StepCache extends Cleanable {
    private static final Predicate<Throwable> EXCEPTION_CHECKER_DEFAULT = t -> false;
    private final LinkedList<Executable> orderedSteps = new LinkedList<>();
    private Executable lastStep;
    private final Multimap<String, Executable> unorderedSteps = LinkedHashMultimap.create();
    private final ListMultimap<String, Executable> sequencedSteps = MultimapBuilder.linkedHashKeys().arrayListValues().build();
    private final Set<String> allUnorderedStepNames = new LinkedHashSet<>();
    private final Set<String> allSequenceStepNames = new LinkedHashSet<>();
    private int stepCounter = 0;
    private final List<Executable> finalSteps = new ArrayList<>();
    private Predicate<Throwable> exceptionChecker = EXCEPTION_CHECKER_DEFAULT;
    private final List<Executable> failingSteps = new ArrayList<>();

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
        sequencedSteps.clear();
        allUnorderedStepNames.clear();
        allSequenceStepNames.clear();
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
        return !unorderedSteps.containsKey(name) && !sequencedSteps.containsKey(name);
    }

    public String getRandomUnorderedStepName() {
        String name = String.valueOf(System.nanoTime());
        while (allUnorderedStepNames.contains(name) || allSequenceStepNames.contains(name)) {
            name = String.valueOf(System.nanoTime());
        }
        return name;
    }

    public void addUnordered(String name, Executable testStep) {
        allUnorderedStepNames.add(name);
        Preconditions.checkState(!allSequenceStepNames.contains(name),
                "Cannot add unordered step with name [%s] as it has already been used for a sequenced step.", name);

        unorderedSteps.put(name, testStep);
    }

    public void addSequenced(String name, Executable testStep) {
        allSequenceStepNames.add(name);
        Preconditions.checkState(!allUnorderedStepNames.contains(name),
                "Cannot add sequenced step with name [%s] as it has already been used for an unordered step.", name);

        if (!sequencedSteps.containsKey(name)) {
            //Sequenced steps should be executed as soon as they reach the head of the queue
            testStep.setActive();
            testStep.executeAndLog();
        }
        if (!testStep.isFinished()) {
            sequencedSteps.put(name, testStep);
        }
    }

    public ImmutableSet<String> getAllUnorderedStepNames() {
        return ImmutableSet.<String>builder()
                .addAll(allUnorderedStepNames)
                .addAll(allSequenceStepNames)
                .build();
    }

    public boolean hasAddedStepWithName(String name) {
        return allUnorderedStepNames.contains(name) || allSequenceStepNames.contains(name);
    }

    public void removeAndCancel(String name) {
        Preconditions.checkState(unorderedSteps.containsKey(name) || sequencedSteps.containsKey(name),
                "Tried to remove unordered or sequenced steps with name '%s', but didn't find one. Known unorderedSteps: %s, sequencedSteps %s",
                name,
                unorderedSteps,
                sequencedSteps);
        removeAndCancelIfPresent(name);
    }

    public void removeAndCancelIfPresent(String name) {
        unorderedSteps.removeAll(name).stream().filter(step -> step instanceof Cancelable).forEach(step -> ((Cancelable) step).cancel());
        sequencedSteps.removeAll(name).stream().filter(step -> step instanceof Cancelable).forEach(step -> ((Cancelable) step).cancel());
    }

    public Iterator<Executable> getUnorderedStepsIterator() {
        return new UnorderedStepsIterator();
    }

    public boolean isFinished() {
        boolean unorderedStepsFinished = Stream.concat(
                        unorderedSteps.values().stream(),
                        sequencedSteps.values().stream())
                .noneMatch(step -> step.isRequired() && !step.isFinished());

        return unorderedStepsFinished && orderedSteps.isEmpty();
    }

    @CheckForNull
    public Executable getUnfinishedUnorderedStep() {
        return Stream.concat(unorderedSteps.values().stream(), sequencedSteps.values().stream())
                .filter(step -> step.isRequired() && !step.isFinished())
                .findAny()
                .orElse(null);
    }

    @Override
    public void clean() {
        orderedSteps.clear();
        unorderedSteps.clear();
        sequencedSteps.clear();
        allUnorderedStepNames.clear();
        allSequenceStepNames.clear();
        finalSteps.clear();
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
        return !orderedSteps.isEmpty() || !unorderedSteps.isEmpty() || !sequencedSteps.isEmpty() || !finalSteps.isEmpty();
    }

    public List<Executable> getFailingSteps() {
        return failingSteps;
    }

    public void addFailingStep(Executable failingStep) {
        this.failingSteps.add(failingStep);
    }

    private class UnorderedStepsIterator implements Iterator<Executable> {
        private final Iterator<Executable> unorderedStepsIterator;
        private Iterator<String> sequenceNames;
        private String currentSequence = null;

        private UnorderedStepsIterator() {
            this.unorderedStepsIterator = unorderedSteps.values().iterator();
            this.sequenceNames = sequencedSteps.keySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return unorderedStepsIterator.hasNext() || sequenceNames.hasNext();
        }

        @Override
        public Executable next() {
            if (unorderedStepsIterator.hasNext()) {
                return unorderedStepsIterator.next();
            }
            currentSequence = sequenceNames.next();
            return sequencedSteps.get(currentSequence).get(0);
        }

        @Override
        public void remove() {
            if (currentSequence == null) {
                unorderedStepsIterator.remove();
            } else {
                advanceSequence(currentSequence);
                this.sequenceNames = sequencedSteps.keySet().iterator(); // Reset this in case the head step from an earlier sequence is now complete
            }
        }

        private void advanceSequence(String currentSequence) {
            sequencedSteps.get(currentSequence).remove(0);
            if (sequencedSteps.containsKey(currentSequence)) {
                sequencedSteps.get(currentSequence).get(0).setActive();
            }
        }
    }
}
