/*
 * Copyright © 2017-2023 Ocado (Ocava)
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
package com.ocadotechnology.fileaccess.serviceloader;

/**
 * {@link RuntimeException} to throw when the {@link DataAccessManager} cannot find the requested {@link com.ocadotechnology.fileaccess.serviceprovider.DataAccessServiceProvider}
 */
public class DataAccessServiceProviderNotAvailableException extends RuntimeException {
    public DataAccessServiceProviderNotAvailableException() {
        super();
    }

    public DataAccessServiceProviderNotAvailableException(String message) {
        super(message);
    }

    public DataAccessServiceProviderNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessServiceProviderNotAvailableException(Throwable cause) {
        super(cause);
    }

    protected DataAccessServiceProviderNotAvailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
