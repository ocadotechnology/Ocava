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
package com.ocadotechnology.scenario;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class NotificationCacheTest {
    @ParameterizedTest
    @MethodSource("getArguments")
    public void allSuperTypesMatch(List<Class<?>> knownNotificationClasses, Object notification, boolean expected) {
        NotificationCache cache = new NotificationCache();
        knownNotificationClasses.forEach(cache::addKnownNotification);

        Assertions.assertEquals(expected, cache.knownNotification(notification));

    }

    private static Stream<Arguments> getArguments() {
        return Stream.of(
                Arguments.arguments(List.of(TopLevelTestNotification.class), new RedCircleNotification(), true),
                Arguments.arguments(List.of(ShapeNotification.class), new RedCircleNotification(), true),
                Arguments.arguments(List.of(CircleNotification.class), new RedCircleNotification(), true),
                Arguments.arguments(List.of(RedCircleNotification.class), new RedCircleNotification(), true),
                Arguments.arguments(List.of(TopLevelTestNotification.class, ShapeNotification.class, CircleNotification.class, RedCircleNotification.class), new RedCircleNotification(), true),
                Arguments.arguments(List.of(TopLevelTestNotification.class, ShapeNotification.class, CircleNotification.class, RedCircleNotification.class), new TestEventNotification("dummy"), false)
        );
    }
}
