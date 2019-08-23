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

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ExecuteStep extends NamedStep implements Executable {
    protected final AtomicBoolean finished = new AtomicBoolean(false);
    
    @Override
    public boolean isRequired() {
        return true;
    }
    
    @Override
    public boolean isFinished() {
        return finished.get();
    }

    @Override
    public void execute() {
        finished.set(true);
        executeStep();
    }
    
    @Override
    public boolean isMergeable() {
        return false;
    }
    
    @Override
    public void merge(Executable step) {}

    protected abstract void executeStep();
}
