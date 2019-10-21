/*
 * Copyright © 2017-2019 Ocado (Ocava)
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
package com.google.common.eventbus;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * A specialized form of EventBus which prevents the bus from swallowing exceptions and sets up the desired Executor and
 * Dispatcher implementations.
 *
 * This implementation has to reside in the google package because it is accessing package-private methods and classes.
 */
public class BlockingEventBus extends EventBus {
    public BlockingEventBus() {
        super("default", MoreExecutors.directExecutor(), Dispatcher.immediate(), (exception, context) -> {});
    }

    /**
     * The standard implementation of EventBus does not permit propagating Exceptions thrown in Subscribers.  We find it
     * far more useful to propagate these exceptions, allowing us to fail-fast on errors.
     */
    @Override
    void handleSubscriberException(Throwable e, SubscriberExceptionContext context) {
        throw new IllegalStateException("Exception encountered in Subscriber", e);
    }
}