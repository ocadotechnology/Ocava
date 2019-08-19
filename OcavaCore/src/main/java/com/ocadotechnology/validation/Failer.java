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
package com.ocadotechnology.validation;

import com.google.common.base.Preconditions;

public final class Failer {
    private static final IllegalStateException exceptionThatWillNeverBeThrown = new IllegalStateException();

    private Failer() {
        throw new UnsupportedOperationException("Static utility class that shouldn't be instantiated");
    }

    /**
     * Generate an return an Exception to be thrown when the application has failed.
     *
     * <p>This is a convenience method for use where by reaching a point in a function, something has gone wrong and an
     * exception should be thrown. This is preferable to just throwing an exception purely for ease of breakpointing.
     * This is because it is easy to breakpoint on Precondition failures, whereas breakpointing on general exception
     * creation would mean hitting some that are expected (thrown and caught by libraries you use - nothing you can do
     * about it).
     *
     * <p>Having this method return a RuntimeException means that you can throw it, meaning that your function will not
     * also demand a return.
     *
     * <p>Usage:
     * <pre>{@code throw Failer.fail("Something has gone terribly wrong");}</pre>
     * Or:
     * <pre>{@code throw Failer.fail("Something has gone terribly wrong with %s and %s", someVariable, someOtherVariable);}</pre>
     *
     * @throws IllegalStateException    Guaranteed
     */
    public static RuntimeException fail(String errorMessage, Object... stringArgs){
        Preconditions.checkState(false, errorMessage, stringArgs);
        return exceptionThatWillNeverBeThrown;
    }

    /**
     * Fail because a value that was expected to be present, wasn't.
     *
     * Typically used with {@link java.util.Optional#orElseThrow(java.util.function.Supplier) the Optional#orElseThrow} method.
     *
     * <p>Usage:
     * <pre>{@code Optional.empty().orElseThrow(Failer::valueExpected)}</pre>
     *
     * @throws IllegalStateException    Guaranteed
     */
    public static IllegalStateException valueExpected() throws IllegalStateException {
        throw Failer.fail("Value expected to be present");
    }
}
