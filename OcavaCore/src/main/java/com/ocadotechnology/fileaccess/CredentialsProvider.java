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
package com.ocadotechnology.fileaccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ocadotechnology.validation.Failer;

public class CredentialsProvider {
    private static final String CREDENTIALS_FILE_NAME = "credentials";
    private static final String CREDENTIALS_DIRECTORY = ".ocava_access_credentials";
    private static final String USER_HOME = "user.home";
    private final String homeDir;

    public CredentialsProvider() {
        homeDir = System.getProperty(USER_HOME);
        Preconditions.checkNotNull(homeDir, "user.home is not set");
    }

    private File getCredentialsDirectory() {
        File credentialsDirectory = new File(homeDir, CREDENTIALS_DIRECTORY);
        Preconditions.checkState(credentialsDirectory.exists(), "Credentials Directory %s does not exist", credentialsDirectory.getAbsolutePath());
        return credentialsDirectory;
    }

    private File getCredentialsFile() {
        File file = new File(getCredentialsDirectory(), CREDENTIALS_FILE_NAME);
        Preconditions.checkState(file.exists(), "Credentials file %s does not exist", file.getAbsolutePath());
        return file;
    }

    /**
     * @return the location of the credentials file which this object will try to read from. Does not perform any
     *          checks as to whether the file exists etc.
     */
    public File getCredentialsFileUnchecked() {
        return new File(new File(homeDir, CREDENTIALS_DIRECTORY), CREDENTIALS_FILE_NAME);
    }

    /**
     * Looks for Credentials to be located in $HomeDir/.ocava-credentials/credentials file
     *
     * @return an ImmutableMap of all the key-value pairs present in the file
     * @throws IllegalArgumentException if the directory or file is not available
     *                                  if the file is present but empty
     */
    public ImmutableMap<String, String> getCredentials() {
        File file = getCredentialsFile();
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            properties.load(inputStream);
            Preconditions.checkArgument(!properties.isEmpty(), "No credentials found in file " + file.getAbsolutePath());
            return Maps.fromProperties(properties);
        } catch (IOException e) {
            throw Failer.fail("Failed to read credentials from file");
        }
    }

}
