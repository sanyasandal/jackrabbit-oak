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
package org.apache.jackrabbit.oak.security.authorization.restriction;

import java.util.Objects;

import javax.jcr.security.AccessControlException;

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionPattern;
import org.apache.jackrabbit.util.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * {@code GlobPattern} defines a simplistic pattern matching. It consists
 * of a mandatory (leading) path and an optional "glob" that may contain one or
 * more wildcard characters ("{@code *}") according to the glob matching
 * defined by {@link javax.jcr.Node#getNodes(String[])}. In contrast to that
 * method the {@code GlobPattern} operates on path (not only names).
 * <p>
 *
 * <p>
 * Please note the following special cases:
 * <pre>
 * NodePath     |   Restriction   |   Matches
 * -----------------------------------------------------------------------------
 * /foo         |   null          |   matches /foo and all descendants of /foo
 * /foo         |   ""            |   matches /foo only (no descendants, not even properties)
 * </pre>
 * </p>
 *
 * <p>
 * Examples without wildcard char:
 * <pre>
 * NodePath = "/foo"
 * Restriction   |   Matches
 * -----------------------------------------------------------------------------
 * /cat          |   '/foo/cat' and all it's descendants
 * /cat/         |   all descendants of '/foo/cat'
 * cat           |   '/foocat' and all it's descendants
 * cat/          |   all descendants of '/foocat'
 * </pre>
 * </p>
 *
 * <p>
 * Examples including wildcard char:
 * <pre>
 * NodePath = "/foo"
 * Restriction   |   Matches
 * -----------------------------------------------------------------------------
 * &#42;         |   foo, all siblings of foo and their descendants
 * /&#42;cat     |   all descendants of /foo whose path ends with "cat"
 * /&#42;/cat    |   all non-direct descendants of /foo named "cat"
 * /cat&#42;     |   all descendant path of /foo that have the direct foo-descendant segment starting with "cat"
 * &#42;cat      |   all siblings and descendants of foo that have a name ending with cat
 * &#42;/cat     |   all descendants of /foo and foo's siblings that have a name segment "cat"
 * cat/&#42;     |   all descendants of '/foocat'
 * /cat/&#42;    |   all descendants of '/foo/cat'
 * &#42;cat/&#42;    |   all siblings and descendants of foo that have an intermediate segment ending with 'cat'
 * /&#42;cat/&#42;   |   all descendants of /foo that have an intermediate segment ending with 'cat'
 * </pre>
 * </p>
 */
final class GlobPattern implements RestrictionPattern {

    private static final char WILDCARD_CHAR = '*';
    private static final int MAX_WILDCARD = 20;

    private final String path;
    private final String restriction;

    private final Pattern pattern;

    private GlobPattern(@NotNull String path, @NotNull String restriction)  {
        this.path = requireNonNull(path);
        this.restriction = restriction;

        if (!restriction.isEmpty()) {
            StringBuilder b = new StringBuilder(path);
            b.append(restriction);

            int lastPos = restriction.lastIndexOf(WILDCARD_CHAR);
            if (lastPos >= 0) {
                String end;
                if (lastPos != restriction.length()-1) {
                    end = restriction.substring(lastPos + 1);
                } else {
                    end = null;
                }
                pattern = new WildcardPattern(b.toString(), end);
            } else {
                pattern = new PathPattern(b.toString());
            }
        } else {
            pattern = new PathPattern(restriction);
        }
    }

    static GlobPattern create(@NotNull String nodePath, @NotNull String restrictions) {
        return new GlobPattern(nodePath, restrictions);
    }

    static void validate(@NotNull String restriction) throws AccessControlException {
        int cnt = 0;
        for (int i = 0; i < restriction.length(); i++) {
            if (WILDCARD_CHAR == restriction.charAt(i)) {
                cnt++;
            }
            if (cnt > MAX_WILDCARD) {
                throw new AccessControlException("Number of wildcards in rep:glob exceeds allowed complexity.");
            }
        }
    }

    //-------------------------------------------------< RestrictionPattern >---
    @Override
    public boolean matches(@NotNull Tree tree, @Nullable PropertyState property) {
        String itemPath = (property == null) ? tree.getPath() : PathUtils.concat(tree.getPath(), property.getName());
        return matches(itemPath);
    }

    @Override
    public boolean matches(@NotNull String path) {
        return pattern.matches(path);
    }

    @Override
    public boolean matches() {
        // repository level permissions never match any glob pattern
        return false;
    }

    //-------------------------------------------------------------< Object >---
    @Override
    public int hashCode() {
        return Objects.hash(path, restriction);
    }

    @Override
    public String toString() {
        return path + " : " + restriction;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof GlobPattern) {
            GlobPattern other = (GlobPattern) obj;
            return path.equals(other.path) &&  restriction.equals(other.restriction);
        }
        return false;
    }

    //------------------------------------------------------< inner classes >---
    /**
     * Base for PathPattern and WildcardPattern
     */
    private interface Pattern {
        boolean matches(@NotNull String toMatch);
    }

    /**
     * Path pattern: The restriction is missing or doesn't contain any wildcard character.
     */
    private final class PathPattern implements Pattern {

        private final String patternStr;

        private PathPattern(@NotNull String patternStr) {
            this.patternStr = patternStr;
        }

        @Override
        public boolean matches(@NotNull String toMatch) {
            if (patternStr.isEmpty()) {
                return path.equals(toMatch);
            } else {
                // no wildcard contained in restriction: use path defined
                // by path + restriction to calculate the match
                return Text.isDescendantOrEqual(patternStr, toMatch);
            }
        }
    }

    /**
     * Wildcard pattern: The specified restriction contains one or more wildcard character(s).
     */
    private final class WildcardPattern implements Pattern {

        private final String patternEnd;
        private final char[] patternChars;

        private WildcardPattern(@NotNull String patternStr, @Nullable String patternEnd) {
            patternChars = patternStr.toCharArray();
            this.patternEnd = patternEnd;
        }

        @Override
        public boolean matches(@NotNull String toMatch) {
            if (patternEnd != null && !toMatch.endsWith(patternEnd)) {
                // shortcut: verify if end of pattern matches end of toMatch
                return false;
            }
            char[] tm = (toMatch.endsWith("/")) ? toMatch.substring(0, toMatch.length()-1).toCharArray() : toMatch.toCharArray();
            // shortcut didn't reveal mismatch -> need to process the internal match method.
            return matches(patternChars, 0, tm, 0, MAX_WILDCARD);
        }

        /**
         *
         * @param pattern The pattern
         * @param pOff
         * @param s
         * @param sOff
         * @return {@code true} if matches, {@code false} otherwise
         */
        private boolean matches(char[] pattern, int pOff,
                                char[] s, int sOff, int cnt) {

            if (cnt <= 0) {
                throw new IllegalArgumentException("Illegal glob pattern " + GlobPattern.this);
            }

            int pLength = pattern.length;
            int sLength = s.length;

            while (true) {
                // end of pattern reached: matches only if sOff points at the end
                // of the string to match.
                if (pOff >= pLength) {
                    return sOff >= sLength;
                }

                // the end of the string to match has been reached but pattern
                // doesn't have '*' at patternIndex -> no match
                if (sOff >= sLength && pattern[pOff] != WILDCARD_CHAR) {
                    return false;
                }

                // the next character of the pattern is '*'
                // -> recursively test if the rest of the specified string matches
                if (pattern[pOff] == WILDCARD_CHAR) {
                    if (++pOff >= pLength) {
                        return true;
                    }

                    cnt--;
                    while (true) {
                        if (matches(pattern, pOff, s, sOff, cnt)) {
                            return true;
                        }
                        if (sOff >= sLength) {
                            return false;
                        }
                        sOff++;
                    }
                }

                // not yet reached end string and not wildcard character.
                // the 2 strings don't match in case the characters at the current
                // position are not the same.
                if (sOff < sLength) {
                    if (pattern[pOff] != s[sOff]) {
                        return false;
                    }
                }
                pOff++;
                sOff++;
            }
        }
    }
}
