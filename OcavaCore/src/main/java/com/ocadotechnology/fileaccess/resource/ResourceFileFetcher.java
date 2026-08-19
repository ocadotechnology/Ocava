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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
 * <p>
 * If the resource is contained within an archive such as a .jar, then a temporary copy of the file will be created and
 * returned. This is to ensure that the file can be treated as a File object if required. The temporary file will be
 * configured to delete on exit.
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
            throw new RuntimeException("Unable to convert file URL " + fileUrl + " to a URI when loading resource " + resourceName, e);
        }

        try {
            return checkFileSystemThenExtractPath(fileUri, resourceName);
        } catch (IOException e) {
            throw new RuntimeException("Unable to convert file URI " + fileUri + " to a Path when loading resource " + resourceName, e);
        }
    }

    // If a resource file is contained in an archive such as a .jar, then the FileSystem to access archived files may
    // not have been loaded.
    // If it hasn't then temporarily load it to extract the Path to the file
    private Path checkFileSystemThenExtractPath(URI fileUri, String resourceName) throws IOException {
        try {
            return extractPathToFileObject(fileUri, resourceName);
        } catch (FileSystemNotFoundException e) {
            try (FileSystem tempFileSystem = FileSystems.newFileSystem(fileUri, Map.of())) {
                return extractPathToFileObject(fileUri, resourceName);
            }
        }
    }

    private Path extractPathToFileObject(URI fileUri, String resourceName) {
        Path filePath = Paths.get(fileUri);

        // Ensure that the file can be treated as a File object. If this is not possible for the original file, such as
        // if it is contained in a .jar archive, then a temporary copy of the file will be created, and the path to this
        // temporary file will be returned
        return extractFile(filePath, resourceName).toPath();
    }

    private File extractFile(Path filePath, String resourceName) {
        try {
            return filePath.toFile();
        } catch (UnsupportedOperationException toFileException) {
            try {
                return createTempFileCopy(filePath, resourceName);
            } catch (IOException tempFileException) {
                throw new RuntimeException("Unable to create temporary file for " + filePath + " when loading resource " + resourceName, tempFileException);
            }
        }
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "filePath.getFileName() won't be handling an empty file path")
    private File createTempFileCopy(Path filePath, String resourceName) throws IOException {
        File tempFile = File.createTempFile(filePath.getFileName().toString(), null);
        tempFile.deleteOnExit();
        try (FileOutputStream tempFileOut = new FileOutputStream(tempFile)) {
            Files.copy(filePath, tempFileOut);
        }
        logger.info("Created temporary file {} for resource {}", tempFile, resourceName);
        return tempFile;
    }
}
