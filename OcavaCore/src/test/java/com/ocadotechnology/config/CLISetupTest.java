/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class CLISetupTest {
    @Test
    public void getRedactedArgs_passedSecretConfig_omitsSecret() {
        String[] args = {"-OTestConfig.FOO=1", "-OTestConfig.SECRET_1=20"};
        CLISetup setup = CLISetup.parseCommandLineArguments(args);
        assertEquals("TestConfig.FOO:1", setup.getRedactedArgs(TestConfig.class));
    }

    @Test
    public void getRedactedArgs_passedSecretSubConfig_omitsSecrets() {
        String[] args = {
                "-OTestConfig.BAR=1",
                "-OTestConfig.BAZ=1",
                "-OTestConfig.DUPLICATED_KEY=1",
                "-OTestConfig.EMPTY=1",
                "-OTestConfig.FOO=1",
                "-OTestConfig.SECRET_1=1",
                "-OTestConfig.Colours.BLUE=1",
                "-OTestConfig.Colours.GREEN=1",
                "-OTestConfig.Colours.RED=1",
                "-OTestConfig.FirstSubConfig.DUPLICATED_KEY=1",
                "-OTestConfig.FirstSubConfig.HOO=1",
                "-OTestConfig.FirstSubConfig.SECRET_2=1",
                "-OTestConfig.FirstSubConfig.WOO=1",
                "-OTestConfig.FirstSubConfig.SubSubConfig.X=1",
                "-OTestConfig.FirstSubConfig.SubSubConfig.Y=1",
                "-OTestConfig.SecondSubConfig.WOO=1",
        };
        CLISetup setup = CLISetup.parseCommandLineArguments(args);
        String expected = Arrays.stream(args)
                .filter(string -> !string.contains("SECRET"))
                .map(this::convertToOutputString)
                .collect(Collectors.joining(" "));
        assertEquals(expected, setup.getRedactedArgs(TestConfig.class));
    }

    @Test
    public void getRedactedArgs_passedConfigWithSpacesInValues_omitsSecret() {
        String[] args = {"-OTestConfig.FOO=(1, 2)", "-OTestConfig.SECRET_1=20"};
        CLISetup setup = CLISetup.parseCommandLineArguments(args);
        assertEquals("TestConfig.FOO:(1, 2)", setup.getRedactedArgs(TestConfig.class));
    }

    @Test
    public void getRedactedArgs_passedInvalidArgs_returnsUsefulMessage() {
        String[] args = {"-OTestConfig.FOO=1", "-OTestConfig.SECRET_22=20"};
        CLISetup setup = CLISetup.parseCommandLineArguments(args);
        assertEquals("Error parsing config keys: The following config keys were not recognised:[TestConfig.SECRET_22]", setup.getRedactedArgs(TestConfig.class));
    }

    private String convertToOutputString(String string) {
        // Strip first two characters, replace "=1" suffix with ":1"
        return string.substring(2, string.length() - 2) + ":1";
    }

}