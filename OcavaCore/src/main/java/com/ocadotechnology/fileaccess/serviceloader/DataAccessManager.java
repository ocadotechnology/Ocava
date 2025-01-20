/*
 * Copyright © 2017-2025 Ocado (Ocava)
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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.DataSourceDefinition;
import com.ocadotechnology.fileaccess.service.DataAccessor;
import com.ocadotechnology.fileaccess.serviceprovider.DataAccessServiceProvider;

/**
 * File Access Manager class to retrieve file based on the mode of access requested
 */
public class DataAccessManager {
    private final ImmutableMap<String, DataAccessServiceProvider> providerMap ;
    private final ImmutableMap<String, Config<?>> initialConfigMap;
    private final HashMap<String, DataAccessor> accessorMap = new HashMap<>();

    /**
     * Loads all the ServiceProviders to access files, available in the class path
     *
     * @param initialConfigMap Map containing modes and their respective initialConfigurations that the application prefers to use
     * @throws ServiceConfigurationError if Multiple ServiceProviders with the same mode are available in classpath
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "This object does not contain data that constitutes a security risk")
    public DataAccessManager(ImmutableMap<String, Config<?>> initialConfigMap) {
        Preconditions.checkNotNull(initialConfigMap, "initial config cannot be null");
        this.providerMap = buildProviderMap();
        this.initialConfigMap = initialConfigMap;
    }

    private ImmutableMap<String, DataAccessServiceProvider> buildProviderMap() {
        HashMap<String, DataAccessServiceProvider> providers = new HashMap<>();
        ServiceLoader<DataAccessServiceProvider> loader = ServiceLoader.load(DataAccessServiceProvider.class);

        loader.forEach(provider -> {
            String mode = provider.getMode();
            if (providers.put(mode, provider) != null) {
                throw new ServiceConfigurationError("More than one service provider found for mode " + mode);
            }
        });
        return  ImmutableMap.copyOf(providers);
    }

    /**
     * Fetches the requested file based on the mode set in the data configuration
     *
     * @throws IllegalStateException if ServiceProvider for the mode requested in the dataConfig is not available
     *                               or if the Service (DataAccessor) for the requested mode is not initialised
     */
    public Path getFileFromConfig(DataSourceDefinition<?> dataSourceDefinition, Config<?> dataConfig, String defaultBucket) {
        String mode = dataConfig.getValue(dataSourceDefinition.mode).asString();
        DataAccessor accessor = accessorMap.computeIfAbsent(mode, this::createAccessor);
        return accessor.getFileFromConfig(dataSourceDefinition, dataConfig, defaultBucket);
    }

    private DataAccessor createAccessor(String mode) {
        if (!providerMap.containsKey(mode)) {
            throw new DataAccessServiceProviderNotAvailableException(
                    "ServiceProvider is not available for mode " + mode
                            + ". Modes with available ServiceProviders: " + providerMap.keySet());
        }
        Preconditions.checkState(initialConfigMap.containsKey(mode), "Accessor is not initialised for mode " + mode);
        return providerMap.get(mode).createAccessor(initialConfigMap.get(mode));
    }
}
