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

import org.junit.jupiter.api.Assertions;

class NeverStep<T> extends UnorderedCheckStep<T> {
    NeverStep(CheckStep<T> wrapped) {
        super(wrapped);
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public void execute() {
        super.execute();
        Assertions.assertFalse(isFinished(),
                "Never condition violated. Step: (" + this + " " + wrapped.info() + ") Notification: " + wrapped.getLastSeen());
    }
}
