/*
 * Copyright © 2017-2021 Ocado (Ocava)
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
package com.ocadotechnology.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

class PointToPointValidatorTest {
    private static class DummySubscriberA {}
    private static class DummySubscriberB {}

    private PointToPointValidator validator;

    @BeforeEach
    void setup() {
        validator = new PointToPointValidator();
    }

    @AfterEach
    void reset() {
        validator.reset();
    }

    /**
     * P2P <-- DummyP2PNotification
     */
    private static class DummyNotification implements Notification {}
    private static class DummyP2PNotification implements Notification, PointToPointNotification {}

    @Test
    void whenNoP2P_thenNoException() {
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummyNotification.class));
        validator.validate(new DummySubscriberB(), ImmutableList.of(DummyNotification.class));
    }

    @Test
    void whenSameClassSubscribesTwice_thenNoException() {
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummyNotification.class));
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummyNotification.class));
    }

    @Test
    void whenSameClassSubscribesTwiceToP2P_thenException() {
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummyP2PNotification.class));
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
                () -> validator.validate(new DummySubscriberA(), ImmutableList.of(DummyP2PNotification.class)));
        Assertions.assertTrue(e.getMessage().contains(DummyP2PNotification.class.getSimpleName()));
    }

    @Test
    void whenTwoP2PSubscribers_thenException() {
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummyP2PNotification.class));
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
                () -> validator.validate(new DummySubscriberB(), ImmutableList.of(DummyP2PNotification.class)));
        Assertions.assertTrue(e.getMessage().contains(DummyP2PNotification.class.getSimpleName()));
    }

    /**
     * DummySupertypeNotification <--+
     *                               |
     * P2P <----------- DummyP2PSubtypeNotification
     */
    private static class DummySupertypeNotification implements Notification {}
    private static class DummyP2PSubtypeNotification extends DummySupertypeNotification implements PointToPointNotification {}

    /**
     * This case is allowed by the weak rule (but not the strong rule) because only one of sub and super is P2P.
     */
    @Test
    void whenSubtypeIsP2P_thenNoException() {
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummySupertypeNotification.class));
        validator.validate(new DummySubscriberB(), ImmutableList.of(DummyP2PSubtypeNotification.class));
    }

    /**
     * This case is allowed by the weak rule (but not the strong rule) because no subscriber directly mentions a P2P.
     */
    @Test
    void whenBothSubscribersForSuperAndSubtypeIsP2P_thenNoException() {
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummySupertypeNotification.class));
        validator.validate(new DummySubscriberB(), ImmutableList.of(DummySupertypeNotification.class));
    }

    /**
     * P2P <-- DummyP2PSupertypeNotification <-- DummySubtypeNotification
     */
    private static class DummyP2PSupertypeNotification implements Notification, PointToPointNotification {}
    private static class DummySubtypeNotification extends DummyP2PSupertypeNotification {}

    /**
     * This case is disallowed by the weak rule because both super and sub are P2P.
     */
    @Test
    void whenSupertypeIsP2P_thenException() {
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummyP2PSupertypeNotification.class));
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
                () -> validator.validate(new DummySubscriberB(), ImmutableList.of(DummySubtypeNotification.class)));
        Assertions.assertTrue(e.getMessage().contains(DummyP2PSupertypeNotification.class.getSimpleName()));
    }

    /**
     * DummySuperTypeInterfaceA <---+
     *                              |
     * P2P <--------- DummyCommonP2PSubtypeNotification
     *                              |
     * DummySuperTypeInterfaceB <---+
     */
    private interface DummySuperTypeInterfaceA extends Notification {}
    private interface DummySuperTypeInterfaceB extends Notification {}
    private static class DummyCommonP2PSubtypeNotification implements DummySuperTypeInterfaceA, DummySuperTypeInterfaceB, PointToPointNotification {}

    /**
     * This case is allowed by the weak rule (but not the strong rule) because no subscriber directly mentions a P2P.
     */
    @Test
    void whenCommonSubtypeIsP2P_thenNoException() {
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummySuperTypeInterfaceA.class));
        validator.validate(new DummySubscriberB(), ImmutableList.of(DummySuperTypeInterfaceB.class));
    }

    /**
     * DummySupertypeNotificationA <---+
     *                                 |
     * P2P <--------- DummyMiddleTypeNotificationB <-- DummySubtypeNotificationC
     */
    private static class DummySupertypeNotificationA implements Notification {}
    private static class DummyMiddleTypeNotificationB extends DummySupertypeNotificationA implements PointToPointNotification {}
    private static class DummySubtypeNotificationC extends DummyMiddleTypeNotificationB {}

    /**
     * This case is allowed by the weak rule (but not the strong rule) because only one of A and C is P2P.
     */
    @Test
    void whenMiddleTypeIsP2P_thenNoException() {
        validator.validate(new DummySubscriberA(), ImmutableList.of(DummySupertypeNotificationA.class));
        validator.validate(new DummySubscriberB(), ImmutableList.of(DummySubtypeNotificationC.class));
    }
}