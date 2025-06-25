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

import static org.junit.platform.commons.util.CollectionUtils.isConvertibleToStream;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

/**
 * Test if the method is a JUnit Jupiter {@linkplain org.junit.jupiter.api.TestFactory} method.
 * This helper class avoids the use of JUnit's internal API.
 */
public class IsTestFactoryMethodPredicate extends IsTestableMethodPredicate {

    private static final Logger logger = LoggerFactory.getLogger(IsTestFactoryMethodPredicate.class);

    public IsTestFactoryMethodPredicate() {
        super(TestFactory.class, IsTestFactoryMethodPredicate::hasCompatibleReturnType);
    }

    private static boolean hasCompatibleReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        if (DynamicNode.class.isAssignableFrom(returnType) || DynamicNode[].class.isAssignableFrom(returnType)) {
            return true;
        }
        if (returnType == Object.class || returnType == Object[].class) {
            logIssueOfHavingTooGenericReturnType(method);
            return true;
        }
        boolean validContainerType = !returnType.isArray() && isConvertibleToStream(returnType);
        return validContainerType && isCompatibleContainerType(method);
    }

    private static boolean isCompatibleContainerType(Method method) {
        Type genericReturnType = method.getGenericReturnType();

        if (genericReturnType instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
            if (typeArguments.length == 1) {
                Type typeArgument = typeArguments[0];
                if (typeArgument instanceof Class) {
                    // Stream<DynamicNode> etc.
                    return DynamicNode.class.isAssignableFrom((Class<?>) typeArgument);
                }
                if (typeArgument instanceof WildcardType) {
                    WildcardType wildcardType = (WildcardType) typeArgument;
                    Type[] upperBounds = wildcardType.getUpperBounds();
                    Type[] lowerBounds = wildcardType.getLowerBounds();
                    if (upperBounds.length == 1 && lowerBounds.length == 0 && upperBounds[0] instanceof Class<?> upperBound) {
                        if (Object.class.equals(upperBound)) { // Stream<?> etc.
                            logIssueOfHavingTooGenericReturnType(method);
                            return true;
                        }
                        // Stream<? extends DynamicNode> etc.
                        return DynamicNode.class.isAssignableFrom(upperBound);
                    }
                }
            }
            return false;
        }

        logIssueOfHavingTooGenericReturnType(method);
        return true;
    }

    private static void logIssueOfHavingTooGenericReturnType(Method method) {
        logger.info(() -> String.format(
                "The declared return type of @TestFactory method '%s' does not support static validation. "
                    + "It must return a single %2$s or a Stream, Collection, Iterable, Iterator, Iterator provider, or array of %2$s.",
                method.toGenericString(),
                DynamicNode.class.getName()));
    }
}
