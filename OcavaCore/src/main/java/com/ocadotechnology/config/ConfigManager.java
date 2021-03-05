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
package com.ocadotechnology.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.physics.units.LengthUnit;

/**
 * Wrapper class to contain a set of unrelated Config objects.
 *
 * Config keys are expected to be defined as a set of nested enum classes giving a hierarchical representation of the
 * data.  Keys should be specified in the data sources with the qualified class names starting at the 'root' config
 * class provided to the ConfigManager.Builder.
 *
 * Eg:
 *
 * {@code
 * public enum SystemConfig {
 *     VALUE_1,
 *     VALUE_2;
 *
 *     public enum SystemSubConfig {
 *         VALUE_3;
 *
 *         public enum HighlyNestedConfig {
 *             VALUE_4
 *         }
 *     }
 * }}
 *
 * The keys would be specified with the strings:
 *
 * {@code "SystemConfig.VALUE_1"}
 * {@code "SystemConfig.VALUE_2"}
 * {@code "SystemConfig.SystemSubConfig.VALUE_3"}
 * {@code "SystemConfig.SystemSubConfig.HighlyNestedConfig.VALUE_4"}
 */
public class ConfigManager {
    public final CLISetup commandLineArgs;
    private final ImmutableMap<Class<? extends Enum<?>>, Config<?>> config;

    private ConfigManager(CLISetup commandLineArgs, ImmutableMap<Class<? extends Enum<?>>, Config<?>> config) {
        this.commandLineArgs = commandLineArgs;
        this.config = config;
    }

    /**
     * Returns a Config object containing all config for root enum clazz.
     *
     * @throws NullPointerException if there is no config defined for this class yet.
     */
    @SuppressWarnings({"unchecked"}) //Effectively checked on insert (see the Builder below)
    public <E extends Enum<E>> Config<E> getConfig(Class<E> clazz) {
        return (Config<E>) Preconditions.checkNotNull(config.get(clazz), "No config loaded for root: %s", clazz.getSimpleName());
    }

    public ImmutableCollection<Config<?>> getAllConfig() {
        return config.values();
    }

    /**
     * For each config, it will bias the config values to match that of the prefix if the prefix value is present,
     * otherwise it will keep its current value
     * @return A new ConfigManager with Config objects that have been biased to use the given prefix
     */
    public ConfigManager getPrefixBiasedConfigManager(String prefix) {
        ImmutableMap<Class<? extends Enum<?>>, Config<?>> biasedConfig = config.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getPrefixBiasedConfigItems(prefix)));

        return new ConfigManager(commandLineArgs, biasedConfig);
    }

    public static class Builder {
        private final CLISetup commandLineArgs;

        private final Map<Class<? extends Enum<?>>, Config<?>> config = new HashMap<>();
        private final ConfigUsageChecker checker = new ConfigUsageChecker();
        private TimeUnit timeUnit = null;
        private LengthUnit lengthUnit = null;
        private ImmutableSet<PrefixedProperty> prefixedProperties;

        /**
         * @param args command line argument overrides for config values.  (@see {@link CLISetup})
         */
        public Builder(String[] args) {
            CLISetup cli = CLISetup.parseCommandLineArguments(args);

            if (cli.hasResourceLocations()) {
                Map<String, String> overrides = new LinkedHashMap<>();
                cli.streamResourceLocations().forEach(loc -> readResource(loc).forEach((k, v) -> overrides.put((String) k, (String) v)));
                overrides.putAll(cli.getOverrides());

                this.commandLineArgs = new CLISetup(cli.getOriginalArgs(), ImmutableMap.copyOf(overrides));
            } else {
                this.commandLineArgs = cli;
            }
        }

        /**
         * Construct ConfigManager.Builder with initial config values from a {@link com.ocadotechnology.config.Config}
         * @param initialConfig initial config
         */
        public Builder(Config<? extends Enum<?>> initialConfig) {
            this.commandLineArgs = CLISetup.parseCommandLineArguments(new String[]{});
            config.put(initialConfig.cls, initialConfig);
        }

        /**
         * Loads the config (for <em>configKey</em>) using command-line parameters.<br>
         * Command-line parameters are automatically included when calling any of the <em>loadConfig</em> methods,
         * but if they are not called for a key, then this is the only way to pull the values from the command-line
         */
        public Builder withConfig(Class<? extends Enum<?>> configKey) {
            return mergePropertiesWithCommandLineOverrides(new Properties(), ImmutableSet.of(configKey));
        }

        /**
         * Loads config from an ordered list of local resource locations, falling back on attempting to load from a file
         * if the resource doesn't exist.  Locations defined later in the list will take precedence over locations
         * earlier in the list.  Command line arguments still take precedence over all locations.
         *
         * @see Builder#loadConfig
         */
        public Builder loadConfigFromResourceOrFile(ImmutableList<String> locations, ImmutableSet<Class<? extends Enum<?>>> configKeys) throws IOException {

            ImmutableList.Builder<ConfigDataSource> builder = ImmutableList.builder();

            for (String location : locations) {
                if (this.getClass().getClassLoader().getResource(location) == null) {
                    builder.add(ConfigDataSource.fromFile(location));
                } else {
                    builder.add(new ConfigDataSource(null, location));
                }
            }
            return loadConfig(builder.build(), configKeys);
        }

        /**
         * Loads config from an ordered list of files only.  Locations defined later in the list will take precedence
         * over locations earlier in the list.  Command line arguments still take precedence over all locations.
         *
         * @see Builder#loadConfig
         */
        public Builder loadConfigFromFiles(ImmutableList<File> files, ImmutableSet<Class<? extends Enum<?>>> configKeys) throws IOException {
            return loadConfig(files.stream().map(ConfigDataSource::fromFile).collect(ImmutableList.toImmutableList()), configKeys);
        }

        /**
         * Loads config from an ordered list of local resource locations only.  Locations defined later in the list will
         * take precedence over locations earlier in the list.  Command line arguments still take precedence over all
         * locations.
         *
         * @see Builder#loadConfig
         */
        public Builder loadConfigFromLocalResources(ImmutableList<String> resources, ImmutableSet<Class<? extends Enum<?>>> configKeys) throws IOException {
            return loadConfig(resources.stream().map(ConfigDataSource::fromLocalResource).collect(ImmutableList.toImmutableList()), configKeys);
        }

        /**
         * Loads config from an ordered list of data sources.  Data sources later in the list will override those
         * earlier on.  Finally any command line arguments will be applied as overrides to values in all of the
         * files and resource locations specified.
         *
         * @throws IOException if any IOException is thrown trying to read from the data sources.
         */
        public Builder loadConfig(ImmutableList<ConfigDataSource> dataSources, ImmutableSet<Class<? extends Enum<?>>> configKeys) throws IOException {
            Properties props = new Properties();
            for (ConfigDataSource dataSource : dataSources) {
                Properties sourceProps = dataSource.fileSource != null
                        ? readFromFile(dataSource.fileSource)
                        : readFromResource(dataSource.resourceLocation);
                props.putAll(sourceProps);
            }

            return mergePropertiesWithCommandLineOverrides(props, configKeys);
        }

        /**
         * Loads config from a pre-constructed map.  Still applies command-line overrides, if any, on top of the map.
         */
        public Builder loadConfigFromMap(ImmutableMap<String, String> data, ImmutableSet<Class<? extends Enum<?>>> configKeys) {
            Properties props = new Properties();
            props.putAll(data);

            return mergePropertiesWithCommandLineOverrides(props, configKeys);
        }

        /**
         * Loads config from environment variables. Still applies command-line overrides, if any, on top of the
         * environment variables.
         *
         * @param configKeys The environment variables to look up. Unlike other config sources, this will not look
         *        for the class name, but just use the enum's raw name.
         *
         * @return The instance of this builder
         * @throws DuplicateMatchingEnvironmentVariableException if a set environment variable matches multiple enum
         *         values across the classes in configKeys.
         */
        public Builder loadConfigFromEnvironmentVariables(ImmutableSet<Class<? extends Enum<?>>> configKeys) {
            return loadConfigFromEnvironmentVariables(System.getenv(), configKeys);
        }

        Builder loadConfigFromEnvironmentVariables(
                Map<String, String> environmentVariables,
                ImmutableSet<Class<? extends Enum<?>>> configKeys
        ) {
            Properties properties = EnvironmentConfigLoader.loadConfigFromEnvironmentVariables(environmentVariables, configKeys);
            return mergePropertiesWithCommandLineOverrides(properties, configKeys);
        }

        private Builder mergePropertiesWithCommandLineOverrides(Properties properties, ImmutableSet<Class<? extends Enum<?>>> configKeys) {
            properties.putAll(commandLineArgs.getOverrides());

            loadPrefixedProperties(properties);

            PropertiesAccessor propertiesAccessor = checker.checkAccessTo(properties);

            //Sacrificing some type safety in order to get past the inability to self-type an enum in a set.
            configKeys.forEach(c -> config.merge(c, ConfigFactory.read((Class<? extends Enum>) c, propertiesAccessor, prefixedProperties), Config::merge));
            return this;
        }

        private void loadPrefixedProperties(Properties properties) {
            ImmutableSet.Builder<PrefixedProperty> prefixedPropertyBuilder = ImmutableSet.builder();

            ImmutableSet<String> prefixedProperties = getPrefixedPropertiesAsStrings(properties.stringPropertyNames());
            for (String prefixedPropertyString : prefixedProperties) {
                PrefixedProperty prefixedProperty = new PrefixedProperty(prefixedPropertyString, properties.getProperty(prefixedPropertyString));
                prefixedPropertyBuilder.add(prefixedProperty);
                properties.setProperty(prefixedProperty.prefixedConfigItem, prefixedProperty.propertyValue);
            }

            this.prefixedProperties = prefixedPropertyBuilder.build();
        }

        ImmutableSet<String> getPrefixedPropertiesAsStrings(Set<String> configItems) {
            return configItems.stream()
                    .filter(configItem -> configItem.contains(ConfigValue.PREFIX_SEPARATOR))
                    .collect(ImmutableSet.toImmutableSet());
        }

        /**
         * Sets the time unit for this application
         */
        public Builder setTimeUnit(TimeUnit timeUnit) {
            EventUtil.setSimulationTimeUnit(timeUnit);
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * Sets the length unit for this application
         */
        public Builder setLengthUnit(LengthUnit lengthUnit) {
            this.lengthUnit = lengthUnit;
            return this;
        }

        /**
         * Allows access to config values during the construction of the config object.  Useful when the location of
         * some config files is itself defined in config, or when logging should be configured before handling any error
         * thrown by the validation.
         *
         * @throws ConfigKeyNotFoundException if a value for the given key has not yet been loaded.
         * @throws IllegalArgumentException if no config object has been created matching the enum key.
         */
        public String getConfigUnchecked(Enum<?> key) {
            return getConfigForKey(key).getValue(key).asString();
        }

        /**
         * Allows access to config values during the construction of the config object.  Useful when the location of
         * some config files is itself defined in config, or when logging should be configured before handling any error
         * thrown by the validation.
         *
         * @throws IllegalArgumentException if no config object has been created matching the enum key.
         */
        public Optional<String> getConfigIfKeyAndValueDefinedUnchecked(Enum<?> key) {
            return getConfigForKey(key).getIfKeyAndValueDefined(key).asString();
        }

        /**
         * Allows access to config objects during their construction.  Further changes to the builder, such as loading
         * config from additional sources will not be reflected in the returned Config object.
         *
         * @throws IllegalArgumentException if no config object has been created matching the enum key.
         */
        public Config<?> getConfigForKey(Enum<?> key) {
            return config.values().stream()
                    .filter(c -> c.enumTypeMatches(key.getClass()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("%s does not belong to any of %s", key, config.keySet())));
        }

        /**
         * Check that the key has been explicitly defined and not to the empty string during the construction of the
         * config object. When true {@link #getConfigUnchecked(Enum)} will not throw an exception and
         * {@link #getConfigIfKeyAndValueDefinedUnchecked(Enum)} will return a populated Optional.
         * When false {@link #getConfigIfKeyAndValueDefinedUnchecked(Enum)} will return {@link Optional#empty()} but
         * {@link #getConfigUnchecked(Enum)} may either throw or return an empty string.
         */
        public boolean areKeyAndValueDefinedUnchecked(Enum<?> key) {
            return config.values().stream().anyMatch(c -> c.areKeyAndValueDefined(key));
        }

        public Set<String> getUnrecognisedProperties() {
            return checker.getUnrecognisedProperties();
        }

        /**
         * Builds a ConfigManager, optionally checking if all properties pass to builder are recognised.
         * The option to disable this check has been added so a ConfigManager can be built ignoring unrecognised
         * properties which can be useful e.g. when using an old config file with unsupported properties.
         *
         * Throws {@link ConfigKeyNotFoundException} if verification is enabled and unrecognised properties are found.
         *
         * @param verifyAllPropertiesRecognised
         * @throws ConfigKeysNotRecognisedException
         */
        public ConfigManager build(boolean verifyAllPropertiesRecognised) throws ConfigKeysNotRecognisedException {
            if (verifyAllPropertiesRecognised) {
                Set<String> unrecognisedProperties = getUnrecognisedProperties();
                if (!unrecognisedProperties.isEmpty()) {
                    throw new ConfigKeysNotRecognisedException("The following config keys were not recognised:" + unrecognisedProperties);
                }
            }
            return new ConfigManager(
                    commandLineArgs,
                    config.entrySet().stream()
                            .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().setUnits(timeUnit, lengthUnit)))
            );
        }

        /**
         * Overloads {@link Builder#build(boolean)} to avoid introducing a breaking change to the API.
         * Identical to calling {@link Builder#build(boolean)} with <code>true</code>.
         *
         * @throws ConfigKeysNotRecognisedException
         */
        public ConfigManager build() throws ConfigKeysNotRecognisedException {
            return build(true);
        }

        private static Properties readResource(String resourceLocation) {
            try {
                return readFromResource(resourceLocation);
            } catch (IOException resourceEx) {
                try {
                    File file = new File(resourceLocation);
                    if (file.isFile() && file.canRead()) {
                        return readFromFile(new File(resourceLocation));
                    }
                } catch (IOException fileEx) {
                    throw new RuntimeException("Unable to read as resource (message:" + resourceEx.getMessage() + ") or as file", fileEx);
                }
                throw new RuntimeException(resourceEx);
            }
        }

        private static Properties readFromResource(String resourceLocation) throws IOException {
            if (resourceLocation.startsWith("/")) {
                resourceLocation = resourceLocation.substring(1);
            }
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
            InputStream in = cl.getResourceAsStream(resourceLocation);
            if (in == null) {
                throw new IOException("unable to load resource " + resourceLocation);
            }
            return readProperties(in);
        }

        private static Properties readFromFile(File fileLocation) throws IOException {
            return readProperties(new FileInputStream(fileLocation));
        }

        private static Properties readProperties(InputStream in) throws IOException {
            Properties props = new Properties();
            props.load(in);
            in.close();
            return props;
        }
    }

    public static class ConfigDataSource {
        private final File fileSource;
        private final String resourceLocation;

        private ConfigDataSource(File fileSource, String resourceLocation) {
            this.fileSource = fileSource;
            this.resourceLocation = resourceLocation;
        }

        public static ConfigDataSource fromFile(String fileLocation) throws IOException {
            File file = new File(fileLocation);
            if (!file.isFile()) {
                throw new IOException("unable to load file " + fileLocation);
            }
            return new ConfigDataSource(Preconditions.checkNotNull(file), null);
        }

        public static ConfigDataSource fromFile(File fileLocation) {
            return new ConfigDataSource(Preconditions.checkNotNull(fileLocation), null);
        }

        public static ConfigDataSource fromLocalResource(String localResource) {
            return new ConfigDataSource(null, Preconditions.checkNotNull(localResource));
        }
    }

    public static class PrefixedProperty {
        final String prefixedConfigItem;
        final String qualifier;
        final String constant;
        final String propertyValue;
        final ImmutableSet<String> prefixes;

        PrefixedProperty(String prefixedConfigItem, String propertyValue) {
            String[] splitPrefixes = prefixedConfigItem.split(ConfigValue.PREFIX_SEPARATOR);
            String[] splitProperties = splitPrefixes[splitPrefixes.length - 1].split("\\.");
            this.prefixedConfigItem = prefixedConfigItem;
            this.propertyValue = propertyValue;
            this.prefixes = Arrays.stream(splitPrefixes)
                    .limit(splitPrefixes.length - 1)
                    .collect(ImmutableSet.toImmutableSet());
            this.constant = splitProperties[splitProperties.length - 1];
            this.qualifier = splitPrefixes[splitPrefixes.length - 1].replace("." + constant, "");
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("qualifier", qualifier)
                    .add("constant", constant)
                    .add("prefixes", prefixes)
                    .toString();

        }
    }
}
