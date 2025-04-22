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
package com.ocadotechnology.junit5.suite.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.reporting.OutputDirectoryProvider;

public class TestOutputDirectoryProvider implements OutputDirectoryProvider {
    private static final Path DUMMY_PATH = Paths.get("dummy");

    public TestOutputDirectoryProvider() {}

    @Override
    public Path getRootDirectory() {
        return DUMMY_PATH;
    }

    @Override
    public Path createOutputDirectory(TestDescriptor testDescriptor) throws IOException {
        return DUMMY_PATH;
    }
}
