/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.oak.index.indexer.document.flatfile.pipelined;

public class SortKeyPath implements Comparable<SortKeyPath> {

    private final String path;
    private final int valuePosition;

    public SortKeyPath(String path, int valuePosition) {
        this.path = path;
        this.valuePosition = valuePosition;
    }

    public int getBufferPos() {
        return valuePosition;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int compareTo(SortKeyPath o) {
        return path.compareTo(o.path);
    }
}
