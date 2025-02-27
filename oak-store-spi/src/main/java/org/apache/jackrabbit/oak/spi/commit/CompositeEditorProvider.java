/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.spi.commit;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aggregation of a list of editor providers into a single provider.
 */
public class CompositeEditorProvider implements EditorProvider {

    private static final EditorProvider EMPTY_PROVIDER =
        new EditorProvider() {
            @Override @Nullable
            public Editor getRootEditor(
                    NodeState before, NodeState after,
                    NodeBuilder builder, CommitInfo info) {
                return null;
            }
        };

    @NotNull
    public static EditorProvider compose(
            @NotNull Collection<? extends EditorProvider> providers) {
        requireNonNull(providers);
        switch (providers.size()) {
            case 0:
                return EMPTY_PROVIDER;
            case 1:
                return providers.iterator().next();
            default:
                return new CompositeEditorProvider(providers);
        }
    }

    private final Collection<? extends EditorProvider> providers;

    private CompositeEditorProvider(
            Collection<? extends EditorProvider> providers) {
        this.providers = providers;
    }

    public CompositeEditorProvider(EditorProvider... providers) {
        this(asList(providers));
    }

    @Override @Nullable
    public Editor getRootEditor(
            NodeState before, NodeState after, NodeBuilder builder,
            CommitInfo info) throws CommitFailedException {
        List<Editor> list = new ArrayList<>(providers.size());
        for (EditorProvider provider : providers) {
            Editor editor = provider.getRootEditor(before, after, builder, info);
            if (editor != null) {
                list.add(editor);
            }
        }
        return CompositeEditor.compose(list);
    }

    @Override
    public String toString() {
        return "CompositeEditorProvider : (" + providers.toString() + ")";
    }
}
