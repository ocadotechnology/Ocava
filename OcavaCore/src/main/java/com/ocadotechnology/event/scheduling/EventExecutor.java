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
package com.ocadotechnology.event.scheduling;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.event.RecoverableException;

public class EventExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Set<Consumer<RecoverableException>> recoverableFailureListeners = new HashSet<>();

    public void registerRecoverableFailureListener(Consumer<RecoverableException> l) {
        recoverableFailureListeners.add(l);
    }

    void execute(Event event, double currentTime) {
        try {
            event.execute();
        } catch (Exception e) {
            if (e instanceof RecoverableException) {
                processRecoverableException((RecoverableException) e, event, currentTime);
                return;
            }
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof RecoverableException) {
                    processRecoverableException((RecoverableException) cause, event, currentTime);
                    break;
                }
                cause = cause.getCause();
            }
            if (cause == null) {
                throw e;
            }
        }
    }

    private void processRecoverableException(RecoverableException e, Event event, double currentTime) {
        logger.error("Replication attempting to recover at {} from failure processing {}", EventUtil.eventTimeToString(currentTime), event, e);
        recoverableFailureListeners.forEach(l -> l.accept(e));
    }
}
