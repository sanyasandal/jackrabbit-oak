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
package org.apache.jackrabbit.oak.segment.azure.tool;

import org.apache.jackrabbit.oak.segment.spi.persistence.SegmentNodeStorePersistence;

public class SegmentCopyAzureToTarTest extends SegmentCopyTestBase {

    @Override
    protected SegmentNodeStorePersistence getSrcPersistence() throws Exception {
        return getAzurePersistence();
    }

    @Override
    protected SegmentNodeStorePersistence getDestPersistence() {
        return getTarPersistence();
    }

    @Override
    protected String getSrcPathOrUri() {
        return getAzurePersistencePathOrUri();
    }

    @Override
    protected String getDestPathOrUri() {
        return getTarPersistencePathOrUri();
    }
}
