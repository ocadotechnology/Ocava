/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

import java.util.HashSet;
import java.util.Set;

/**
 * Class used to register all components which need to be statically cleaned between tests.  Instantiating a Cleanable
 * will add it to the collection of components which will be cleaned after this test is complete.  Cleanable instances
 * must be created afresh for each test run.
 */
public abstract class Cleanable {
    private static Set<Cleanable> cleanables = new HashSet<>();

    public Cleanable() {
        cleanables.add(this);
    }

    public static void cleanAll() {
        cleanables.forEach(Cleanable::clean);
        cleanables = new HashSet<>();
    }

    public abstract void clean();
}
