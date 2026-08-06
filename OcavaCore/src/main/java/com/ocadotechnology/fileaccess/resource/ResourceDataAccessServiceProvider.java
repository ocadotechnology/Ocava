/*
 * Copyright © 2017-2026 Ocado (Ocava)
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
package com.ocadotechnology.fileaccess.resource;

import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.service.DataAccessor;
import com.ocadotechnology.fileaccess.serviceprovider.DataAccessServiceProvider;

/**
 * ServiceProvider to access files in Java resources.
 * <p>
 * To configure this data access service you should {@link com.ocadotechnology.fileaccess.DataSourceDefinition#localFile}
 * with the desired resource name.
 * <p>
 * The resource will then internally be retrieved using Guava's {@link com.google.common.io.Resources#getResource(String)}
 * method.
 * This will for example find a resource on the current class path in simple environments.
 * This also depends on where {@link com.ocadotechnology.fileaccess.serviceloader.DataAccessManager#getFileFromConfig}
 * has been called from.
 */
public class ResourceDataAccessServiceProvider implements DataAccessServiceProvider {
    public static final String MODE = "RESOURCE";

    @Override
    public DataAccessor createAccessor(Config<?> initialConfig) {
        return new ResourceFileFetcher();
    }

    @Override
    public String getMode() {
        return MODE;
    }
}