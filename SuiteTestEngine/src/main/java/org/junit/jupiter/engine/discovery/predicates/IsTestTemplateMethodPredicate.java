/*
 * Copyright © 2017-2025 Ocado (Ocava)
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
package org.junit.jupiter.engine.discovery.predicates;

import org.junit.jupiter.api.TestTemplate;

/**
 * Test if the method is a JUnit Jupiter {@linkplain org.junit.jupiter.api.TestTemplate} method.
 * This helper class avoids the use of JUnit's internal API.
 */
public class IsTestTemplateMethodPredicate extends IsTestableMethodPredicate {

    public IsTestTemplateMethodPredicate() {
        super(TestTemplate.class, method -> method.getReturnType().equals(Void.TYPE));
    }
}