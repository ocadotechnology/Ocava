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
package com.ocadotechnology.event;

import java.io.Serializable;

public abstract class RecoverableException extends RuntimeException {
    private static final long serialVersionUID = 2L;

    public final boolean automaticallyRestart;

    public RecoverableException(boolean automaticallyRestart) {
        this.automaticallyRestart = automaticallyRestart;
    }

    public RecoverableException(String message, boolean automaticallyRestart) {
        super(message);
        this.automaticallyRestart = automaticallyRestart;
    }

    public RecoverableException(String message, Throwable cause, boolean automaticallyRestart) {
        super(message, cause);
        this.automaticallyRestart = automaticallyRestart;
    }

    public RecoverableException(Throwable cause, boolean automaticallyRestart) {
        super(cause);
        this.automaticallyRestart = automaticallyRestart;
    }

    public abstract Serializable getReason();
}
