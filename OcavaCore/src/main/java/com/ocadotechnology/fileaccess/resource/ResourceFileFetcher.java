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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.DataSourceDefinition;
import com.ocadotechnology.fileaccess.service.DataAccessor;

/**
 * DataAccessor which fetches a file from Java resources.
 * <p>
 * The resource name is configured from {@link com.ocadotechnology.fileaccess.DataSourceDefinition#localFile}.
 * <p>
 * Internally Guava's {@link com.google.common.io.Resources#getResource(String)} method finds the resource.
 */
public class ResourceFileFetcher implements DataAccessor {
    private static final Logger logger = LoggerFactory.getLogger(ResourceFileFetcher.class);

    public ResourceFileFetcher() {
    }

    @Override
    public Path getFileFromConfig(DataSourceDefinition<?> dataSourceDefinition, Config<?> dataConfig, String defaultBucket) {
        String resourceName = dataConfig.getValue(dataSourceDefinition.localFile).asString();

        URL fileUrl = Resources.getResource(resourceName);
        URI fileUri;
        try {
            fileUri = fileUrl.toURI();
        } catch (URISyntaxException e) {
            logger.error("Unable to convert file URL {} to a Path when loading resource {}.", fileUrl, resourceName);
            return null;
        }

        return Path.of(fileUri);
    }
}
