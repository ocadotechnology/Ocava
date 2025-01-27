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

import javax.annotation.CheckForNull;

public class TestFileManager extends FileManager {
    private final boolean returnTrueFromVerifyFileSize;
    private final Runnable callback;

    private boolean fileHasBeenRetrievedAndWritten;

    TestFileManager(@CheckForNull FileCache fileCache, boolean returnTrueFromVerifyFileSize, Runnable callback) {
        super(fileCache);
        this.returnTrueFromVerifyFileSize = returnTrueFromVerifyFileSize;
        this.callback = callback;
        this.fileHasBeenRetrievedAndWritten = false;
    }

    TestFileManager(@CheckForNull FileCache fileCache) {
        this(fileCache, true, () -> {});
    }

    TestFileManager(@CheckForNull FileCache fileCache, boolean returnTrueFromVerifyFileSize) {
        this(fileCache, returnTrueFromVerifyFileSize, () -> {});
    }

    TestFileManager(@CheckForNull FileCache fileCache, Runnable callback) {
        this(fileCache, true, callback);
    }

    @Override
    protected boolean verifyFileSize(File cachedFile, String bucket, String key) {
        return returnTrueFromVerifyFileSize;
    }

    @Override
    protected void getFileAndWriteToDestination(String bucket, String key, File writeableFileHandle) {
        try {
            writeableFileHandle.createNewFile();
            fileHasBeenRetrievedAndWritten = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected File applyCallbackAndGetFile(File file) {
        callback.run();
        return file;
    }

    // Used to check if a file has been retrieved and written instead of being found in the cache
    boolean hasFileBeenRetrievedAndWritten() {
        return fileHasBeenRetrievedAndWritten;
    }
}
