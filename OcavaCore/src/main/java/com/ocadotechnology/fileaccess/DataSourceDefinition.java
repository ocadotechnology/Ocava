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

import javax.annotation.CheckForNull;

public class DataSourceDefinition <E extends Enum<E>> {
    /**
     * Config key indicating the mode which should be used to find the file.
     */
    public final E mode;
    /**
     * Config key indicating the local file path, either absolute or relative to a root data directory.
     */
    public final E localFile;
    /**
     * Config key indicating an optional S3 bucket override.
     */
    public final E bucket;
    /**
     * Config key indicating the s3 key of the desired file.
     */
    public final E key;
    /**
     * Config key indicating the full path to a remote file. Intended to take precedence over the individual bucket and key config.
     * To preserve backwards compatibility this may be null.
     */
    @CheckForNull
    public final E remoteFile;

    /**
     * Constructor provided for backwards compatibility with applications which don't specify remoteFile
     */
    public DataSourceDefinition(E mode, E localFile, E bucket, E key) {
        this(mode, localFile, bucket, key, null);
    }

    public DataSourceDefinition(E mode, E localFile, E bucket, E key, E remoteFile) {
        this.mode = mode;
        this.localFile = localFile;
        this.bucket = bucket;
        this.key = key;
        this.remoteFile = remoteFile;
    }
}

