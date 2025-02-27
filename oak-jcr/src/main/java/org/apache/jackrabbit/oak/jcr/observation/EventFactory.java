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
package org.apache.jackrabbit.oak.jcr.observation;

import static org.apache.jackrabbit.guava.common.collect.Iterables.isEmpty;
import static org.apache.jackrabbit.guava.common.collect.Iterables.toArray;
import static java.util.Collections.emptyMap;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.jcr.Value;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.guava.common.base.MoreObjects;

import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.blob.BlobAccessProvider;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.jackrabbit.oak.namepath.NamePathMapper;
import org.apache.jackrabbit.oak.plugins.value.jcr.PartialValueFactory;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Event factory for generating JCR event instances that are optimized
 * for minimum memory overhead. Each factory instance keeps track of the
 * event information (like the user identifier and the commit timestamp)
 * shared across all events from a single commit. The generated events
 * instances postpone things like path mappings and the construction of
 * the event info maps to as late as possible to avoid the memory overhead
 * of keeping track of pre-computed values.
 */
public class EventFactory {
    public static final String USER_DATA = "user-data";

    private final NamePathMapper mapper;

    private final PartialValueFactory valueFactory;

    private final String userID;

    private final String userData;

    private final long date;

    private final boolean external;

    EventFactory(@NotNull NamePathMapper mapper,
                 @NotNull BlobAccessProvider blobAccessProvider,
                 @NotNull CommitInfo commitInfo) {
        this.mapper = mapper;
        this.valueFactory = new PartialValueFactory(mapper, blobAccessProvider);
        if (!commitInfo.isExternal()) {
            this.userID = commitInfo.getUserId();
            Object userData = commitInfo.getInfo().get(USER_DATA);
            this.userData = userData instanceof String ? (String) userData : null;
            this.date = commitInfo.getDate();
            this.external = false;
        } else {
            this.userID = commitInfo.getUserId();
            this.userData = null;
            this.date = commitInfo.getDate(); // rough estimate
            this.external = true;
        }
    }

    Event propertyAdded(
            final PropertyState after, final String primaryType, final Iterable<String> mixinTypes,
            String path, String name, String identifier) {
        return new EventImpl(path, name, identifier) {
            @Override
            public int getType() {
                return PROPERTY_ADDED;
            }
            @Override
            public Map<?, ?> getInfo() {
                Map<Object, Object> builder = new HashMap<>();
                builder.putAll(createInfoMap(primaryType, mixinTypes));
                builder.put("afterValue", createValue(after));
                return Collections.unmodifiableMap(builder);
            }
        };
    }

    Event propertyChanged(
            final PropertyState before, final PropertyState after,
            final String primaryType, final Iterable<String> mixinTypes,
            String path, String name, String identifier) {
        return new EventImpl(path, name, identifier) {
            @Override
            public int getType() {
                return PROPERTY_CHANGED;
            }
            @Override
            public Map<?, ?> getInfo() {
                Map<Object, Object> builder = new HashMap<>();
                builder.putAll(createInfoMap(primaryType, mixinTypes));
                builder.put("beforeValue", createValue(before));
                builder.put("afterValue", createValue(after));
                return Collections.unmodifiableMap(builder);
            }
        };
    }

    Event propertyDeleted(
            final PropertyState before, final String primaryType, final Iterable<String> mixinTypes,
            String path, String name, String identifier) {
        return new EventImpl(path, name, identifier) {
            @Override
            public int getType() {
                return PROPERTY_REMOVED;
            }
            @Override
            public Map<?, ?> getInfo() {
                Map<Object, Object> builder = new HashMap<>();
                builder.putAll(createInfoMap(primaryType, mixinTypes));
                builder.put("beforeValue", createValue(before));
                return Collections.unmodifiableMap(builder);
            }
        };
    }

    private Object createValue(PropertyState property) {
        if (property.isArray()) {
            List<Value> values = valueFactory.createValues(property);
            return values.toArray(new Value[values.size()]);
        } else {
            return valueFactory.createValue(property);
        }
    }

    Event nodeAdded(final String primaryType, final Iterable<String> mixinTypes,
            String path, String name, String identifier) {
        return new EventImpl(path, name, identifier) {
            @Override
            public int getType() {
                return NODE_ADDED;
            }
            @Override
            public Map<?, ?> getInfo() {
                return createInfoMap(primaryType, mixinTypes);
            }
        };
    }

    Event nodeDeleted(final String primaryType, final Iterable<String> mixinTypes,
            String path, String name, String identifier) {
        return new EventImpl(path, name, identifier) {
            @Override
            public int getType() {
                return NODE_REMOVED;
            }
            @Override
            public Map<?, ?> getInfo() {
                return createInfoMap(primaryType, mixinTypes);
            }
        };
    }

    Event nodeMoved(
            final String primaryType, final Iterable<String> mixinTypes,
            String parent, String name, String identifier,
            final String sourcePath) {
        return new EventImpl(parent, name, identifier) {
            @Override
            public int getType() {
                return NODE_MOVED;
            }
            @Override
            public Map<?, ?> getInfo() {
                Map<Object, Object> builder = new HashMap<>();
                builder.put("srcAbsPath", mapper.getJcrPath(sourcePath));
                builder.put("destAbsPath", getPath());
                builder.putAll(createInfoMap(primaryType, mixinTypes));
                return Collections.unmodifiableMap(builder);
            }
        };
    }

    Event nodeReordered(
            final String primaryType, final Iterable<String> mixinTypes,
            String parent, String name, String identifier,
            final String destName) {
        return new EventImpl(parent, name, identifier) {
            @Override
            public int getType() {
                return NODE_MOVED;
            }
            @Override
            public Map<?, ?> getInfo() {
                Map<Object, Object> builder = new HashMap<>();
                builder.put("srcChildRelPath", mapper.getJcrName(name));
                builder.put("destChildRelPath", mapper.getJcrName(destName));
                builder.putAll(createInfoMap(primaryType, mixinTypes));
                return Collections.unmodifiableMap(builder);
            }
        };
    }

    private Map<String, ?> createInfoMap(String primaryType, Iterable<String> mixinTypes) {
        if (isEmpty(mixinTypes)) {
            return Map.of(
                    JCR_PRIMARYTYPE, mapper.getJcrName(primaryType));
        } else {
            List<String> jcrNames = new ArrayList<>();
            for (String name : mixinTypes) {
                jcrNames.add(mapper.getJcrName(name));
            }
            return Map.of(
                    JCR_PRIMARYTYPE, mapper.getJcrName(primaryType),
                    JCR_MIXINTYPES, toArray(jcrNames, String.class));
        }
    }

    //---------------------------------------------------------< EventImpl >--

    private abstract class EventImpl implements JackrabbitEvent {

        /**
         * Path of the parent node of the item this event is about.
         */
        private final String parent;

        /**
         * Name of the item this event is about.
         */
        protected final String name;

        private final String identifier;

        EventImpl(String parent, String name, String identifier) {
            this.parent = parent;
            this.name = name;
            this.identifier = identifier;
        }

        //---------------------------------------------------------< Event >--

        @Override
        public String getPath() {
            return PathUtils.concat(
                    mapper.getJcrPath(parent), mapper.getJcrName(name));
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public Map<?, ?> getInfo() {
            return emptyMap();
        }

        @Override
        public String getUserID() {
            return userID;
        }

        @Override
        public String getUserData() {
            return userData;
        }

        @Override
        public long getDate() {
            return date;
        }

        //-----------------------------------------------< JackrabbitEvent >--

        @Override
        public boolean isExternal() {
            return external;
        }

        //--------------------------------------------------------< Object >--

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object instanceof EventImpl) {
                EventImpl that = (EventImpl) object;
                return getType() == that.getType()
                        && getPath().equals(that.getPath())
                        && getIdentifier().equals(that.getIdentifier())
                        && getInfo().equals(that.getInfo())
                        && Objects.equals(getUserID(), that.getUserID())
                        && Objects.equals(getUserData(), that.getUserData())
                        && getDate() == that.getDate()
                        && isExternal() == that.isExternal();
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    getType(), getPath(), getIdentifier(), getInfo(),
                    getUserID(), getUserData(), getDate(), isExternal());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper("Event")
                    .add("type", getType())
                    .add("path", getPath())
                    .add("identifier", getIdentifier())
                    .add("info", getInfo())
                    .add("userID", getUserID())
                    .add("userData", getUserData())
                    .add("date", getDate())
                    .add("external", isExternal())
                    .toString();
        }

    }

}
