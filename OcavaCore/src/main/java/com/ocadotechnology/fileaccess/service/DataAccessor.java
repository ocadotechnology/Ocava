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
package com.ocadotechnology.fileaccess.service;

import java.nio.file.Path;

import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.DataSourceDefinition;

public interface DataAccessor {
    /**
     * Retrieves the file requested using the preferred mode set in the dataConfig
     *
     * @param dataSourceDefinition defines the set of config keys for the data source
     * @param dataConfig contains the values for the keys defined in dataSourceDefinition
     * @param defaultBucket default bucket to look for the file if a bucket value is not provided inside dataconfig
     * @return Path object for the file requested
     */
    Path getFileFromConfig(DataSourceDefinition<?> dataSourceDefinition, Config<?> dataConfig, String defaultBucket);

}
