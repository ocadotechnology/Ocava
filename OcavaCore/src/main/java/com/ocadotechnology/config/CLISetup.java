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
package com.ocadotechnology.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * All config can be overridden in the form {@code '-O<key>=<value>'} overrides such that they can utilise our hierarchical override mechanism.
 */
@Parameters(separators = " :=")
public class CLISetup {
    @DynamicParameter(
            names = "-O",
            description = "Optional config file overrides")
    private Map<String, String> overrides;

    @Parameter(
            names = "-a",
            splitter = StringListSplitter.class,
            description = "Optional command-line overrides via properties file(s).  Semicolon separated.")
    private List<String> commandLineOverrides = new ArrayList<>();

    private final String commandLine;

    private CLISetup(String[] args) {
        this.commandLine = String.join(" ", Arrays.asList(args));
        this.overrides = new LinkedHashMap<>();  // iteration order same as args order
    }

    public CLISetup(String commandLine, ImmutableMap<String, String> overrides) {
        this.commandLine = commandLine;
        this.overrides = overrides;
    }

    public static CLISetup parseCommandLineArguments(String[] args) {
        CLISetup setup = new CLISetup(args);
        new JCommander(setup, args);
        return setup;
    }

    public ImmutableMap<String, String> getOverrides() {
        return ImmutableMap.copyOf(overrides);
    }

    public String getOriginalArgs() {
        return commandLine;
    }

    public String getRedactedArgs(Class<? extends Enum<?>> configClass) {
        return getRedactedArgs(ImmutableSet.of(configClass));
    }

    public String getRedactedArgs(ImmutableSet<Class<? extends Enum<?>>> configClasses) {
        try {
            ConfigManager cm = new ConfigManager.Builder(commandLine.split(" (?=-O)"))
                    .loadConfigFromEnvironmentVariables(ImmutableMap.of(), configClasses)
                    .build();
            return cm.getAllConfig().stream()
                    .flatMap(config -> config.getKeyValueStringMapWithoutSecrets().entrySet().stream())
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(" "));
        } catch (ConfigKeysNotRecognisedException e) {
            return "Error parsing config keys: " + e.getMessage();
        }
    }

    public boolean hasResourceLocations() {
        return !commandLineOverrides.isEmpty();
    }

    public Stream<String> streamResourceLocations() {
        return commandLineOverrides.stream();
    }

    public static class StringListSplitter implements IParameterSplitter {
        @Override
        public List<String> split(String s) {
            return Arrays.asList(s.split(";"));
        }
    }
}
