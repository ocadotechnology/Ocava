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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.platform.commons.support.AnnotationSupport;

class IsTestableMethodPredicate implements Predicate<Method> {

    private final Class<? extends Annotation> annotationType;
    private final Function<Method, Boolean> returnTypeCheck;

    IsTestableMethodPredicate(Class<? extends Annotation> annotationType, Function<Method, Boolean> returnTypeCheck) {
        this.annotationType = annotationType;
        this.returnTypeCheck = returnTypeCheck;
    }

    @Override
    public boolean test(Method method) {
        if (!AnnotationSupport.isAnnotated(method, annotationType)) {
            return false;
        }
        if (Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        if (Modifier.isPrivate(method.getModifiers())) {
            return false;
        }
        if (Modifier.isAbstract(method.getModifiers())) {
            return false;
        }
        if (!returnTypeCheck.apply(method)) {
            return false;
        }
        return true;
    }
}
