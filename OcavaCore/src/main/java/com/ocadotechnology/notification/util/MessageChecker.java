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
package com.ocadotechnology.notification.util;

import java.util.List;
import java.util.Queue;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.notification.Subscriber;
import com.ocadotechnology.utils.Either;

public class MessageChecker<N> implements Subscriber {

    private final Queue<Either<Class<? extends N>, Predicate<N>>> expectationSequence = Lists.newLinkedList();
    private final List<Either<Class<? extends N>, Predicate<N>>> observedSequence = Lists.newLinkedList();

    private MessageChecker() {
        NotificationRouter.get().addHandler(this);
    }

    public static <N> MessageChecker<N> on() {
        return new MessageChecker<>();
    }

    public MessageChecker<N> expect(Class<? extends N> notificationClass) {
        expectationSequence.add(Either.left(notificationClass));
        return this;
    }

    public MessageChecker<N> expect(Predicate<N> predicate) {
        expectationSequence.add(Either.right(predicate));
        return this;
    }

    @Subscribe
    public void anyNotification(N n) {
        if (expectationSequence.isEmpty()) {
            return;
        }

        if (expectationSequence.peek().testEither(left -> left.equals(n.getClass()), right -> right.apply(n))) {
            observedSequence.add(expectationSequence.poll());
        }
    }

    public void verify() {
        Preconditions.checkState(expectationSequence.isEmpty(),
                "Expected " + expectationSequence.peek());
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return null;
    }
}
