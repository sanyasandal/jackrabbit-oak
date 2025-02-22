/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.security.user;

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.plugins.tree.TreeLocation;
import org.apache.jackrabbit.oak.spi.security.Context;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.plugins.tree.TreeUtil;
import org.apache.jackrabbit.util.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class UserContext implements Context, UserConstants {

    private static final Context INSTANCE = new UserContext();

    private UserContext() {
    }

    static Context getInstance() {
        return INSTANCE;
    }

    //------------------------------------------------------------< Context >---
    @Override
    public boolean definesProperty(@NotNull Tree parent, @NotNull PropertyState property) {
        String propName = property.getName();
        String ntName = TreeUtil.getPrimaryTypeName(parent);
        if (NT_REP_USER.equals(ntName) || NT_REP_SYSTEM_USER.equals(ntName)) {
            return USER_PROPERTY_NAMES.contains(propName);
        } else if (NT_REP_GROUP.equals(ntName)) {
            return GROUP_PROPERTY_NAMES.contains(propName);
        } else if (NT_REP_PASSWORD.equals(ntName)) {
            return PWD_PROPERTY_NAMES.contains(propName);
        } else if (NT_REP_MEMBER_REFERENCES.equals(ntName)) {
            return REP_MEMBERS.equals(propName);
        } else return NT_REP_MEMBERS.equals(ntName);
    }

    @Override
    public boolean definesContextRoot(@NotNull Tree tree) {
        return definesTree(tree);
    }

    @Override
    public boolean definesTree(@NotNull Tree tree) {
        String ntName = TreeUtil.getPrimaryTypeName(tree);
        return Objects.nonNull(ntName) && NT_NAMES.contains(ntName);
    }

    @Override
    public boolean definesLocation(@NotNull TreeLocation location) {
        Tree tree = location.getTree();
        if (tree != null && location.exists()) {
            PropertyState p = location.getProperty();
            return (p == null) ? definesTree(tree) : definesProperty(tree, p);
        } else {
            String path = location.getPath();
            String name = Text.getName(path);
            // NOTE: if none of the conditions below match, we are not able to
            // reliably determine if the specified location defines a user or
            // group node (missing node type information on non-existing location)
            return USER_PROPERTY_NAMES.contains(name)
                    || GROUP_PROPERTY_NAMES.contains(name)
                    || path.contains(REP_MEMBERS_LIST)
                    || path.contains(REP_MEMBERS)
                    || path.contains(REP_PWD);
        }
    }

    @Override
    public boolean definesInternal(@NotNull Tree tree) {
        return false;
    }

}
