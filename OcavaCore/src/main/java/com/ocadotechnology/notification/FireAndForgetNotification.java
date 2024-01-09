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
package com.ocadotechnology.notification;

/**
 * Marker interface for notifications that are intended
 * to be used in fire-and-forget fashion i.e. there
 * may be any number of subscribers including zero.
 *
 * The purpose of having this interface is to allow
 * the creator of a notification to declare that it
 * should not be used for control flow because there
 * may be zero subscribers.
 *
 * A common usage of this interface would be for
 * statistics sent to monitoring classes.
 */
public interface FireAndForgetNotification {
}
