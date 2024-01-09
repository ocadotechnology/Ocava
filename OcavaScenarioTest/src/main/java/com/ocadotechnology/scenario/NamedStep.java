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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NamedStep implements Executable {
    private final Logger logger = LoggerFactory.getLogger(NamedStep.class);

    private Class<?> notificationType;
    private String stepName;
    private int stepOrder;
    private String name = "";
    private boolean hasBeenExecuted = false;
    
    public NamedStep() {
    }

    public NamedStep(Class<?> notificationType) {
        this.notificationType = notificationType;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStepName() {
        return stepName;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void executeAndLog() {
        if (!hasBeenExecuted) {
            logger.info("Executing Step: {}", this);
            hasBeenExecuted = true;
        }
        execute();
    }

    public abstract void execute();

    @Override
    public String toString() {
        String notification = notificationType != null ? notificationType.getSimpleName() : "";
        return String.format("%s %s %s %s %s", stepOrder, stepName, name, notification, info());
    }
    
    protected String info() {
        return "";
    }

    @Override
    public void setActive() {
        //Most implementations of NamedStep do not need to be notified when they become active.
    }
}
