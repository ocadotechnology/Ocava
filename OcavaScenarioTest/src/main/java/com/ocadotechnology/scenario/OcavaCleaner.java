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

import com.ocadotechnology.id.IdGenerator;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.random.RepeatableRandom;

public class OcavaCleaner extends Cleanable {
    public static void register() {
        new OcavaCleaner(); //Constructing a Cleanable object registers it with Cleanable.cleanAll()
    }

    private OcavaCleaner() {}

    @Override
    public void clean() {
        NotificationRouter.get().clearAllHandlers();
        RepeatableRandom.clear();
        IdGenerator.clear();
    }
}
