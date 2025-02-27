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
package org.apache.jackrabbit.oak.run;

import java.util.List;

import org.apache.jackrabbit.guava.common.io.Closer;

import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStoreBuilder;
import org.apache.jackrabbit.oak.plugins.document.check.DocumentStoreCheck;
import org.apache.jackrabbit.oak.run.commons.Command;
import org.apache.jackrabbit.oak.spi.blob.MemoryBlobStore;

import joptsimple.OptionSpec;

/**
 * <code>DocumentStoreCheckCommand</code>...
 */
class DocumentStoreCheckCommand implements Command {

    static final String NAME = "documentstore-check";

    @Override
    public void execute(String... args) throws Exception {
        Closer closer = Utils.createCloserWithShutdownHook();

        String h = NAME + " mongodb://host:port/database|jdbc:... ";

        try {
            CheckOptions options = new CheckOptions(h).parse(args);
            if (options.isHelp()) {
                options.printHelpOn(System.out);
                System.exit(0);
            }

            DocumentNodeStoreBuilder<?> builder = Utils.createDocumentMKBuilder(options, closer);

            if (builder == null) {
                System.err.println("This check is only available for DocumentNodeStore backed by MongoDB or RDB persistence");
                System.exit(1);
            }

            // use a dummy blob store. this command does not check binary properties
            builder.setBlobStore(new MemoryBlobStore());
            builder.setReadOnlyMode();

            DocumentNodeStore dns = builder.build();
            closer.register(Utils.asCloseable(dns));

            new DocumentStoreCheck.Builder(dns, builder.getDocumentStore(), closer)
                    .withOutput(options.getOutput())
                    .withOrphan(options.withOrphan())
                    .withBaseVersion(options.withBaseVersion())
                    .withVersionHistory(options.withVersionHistory())
                    .withPredecessors(options.withPredecessors())
                    .withSuccessors(options.withSuccessors())
                    .withUuid(options.withUuid())
                    .withConsistency(options.withConsistency())
                    .withProgress(options.withProgress())
                    .isSilent(options.isSilent())
                    .withSummary(options.withSummary())
                    .withCounter(options.withCounter())
                    .withNumThreads(options.getNumThreads())
                    .withPaths(options.getPaths())
                    .build().run();

        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    private static final class CheckOptions extends Utils.NodeStoreOptions {

        final OptionSpec<String> out;

        final OptionSpec<Boolean> progress;

        final OptionSpec<Void> silent;

        final OptionSpec<Boolean> summary;

        final OptionSpec<Boolean> counter;

        final OptionSpec<Boolean> orphan;

        final OptionSpec<Boolean> baseVersion;

        final OptionSpec<Boolean> versionHistory;

        final OptionSpec<Boolean> predecessors;

        final OptionSpec<Boolean> successors;

        final OptionSpec<Boolean> uuid;

        final OptionSpec<Boolean> consistency;

        final OptionSpec<Integer> numThreads;

        final OptionSpec<String> paths;

        public CheckOptions(String usage) {
            super(usage);

            out = parser.accepts("out", "Write output to this file")
                    .withRequiredArg();
            silent = parser.accepts("silent", "Do not write output to stdout");
            progress = parser.accepts("progress", "Write periodic progress messages")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            summary = parser.accepts("summary", "Write a summary message at the end")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            counter = parser.accepts("counter", "Count documents and nodes that exist")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            orphan = parser.accepts("orphan", "Check for orphaned nodes")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            baseVersion = parser.accepts("baseVersion", "Check jcr:baseVersion reference")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            versionHistory = parser.accepts("versionHistory", "Check jcr:versionHistory reference")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            predecessors = parser.accepts("predecessors", "Check jcr:predecessors references")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            successors = parser.accepts("successors", "Check jcr:successors references")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            uuid = parser.accepts("uuid", "Check UUID index entry")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            consistency = parser.accepts("consistency", "Check node state consistency")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(Boolean.TRUE);
            numThreads = parser.accepts("numThreads", "Use this number of threads to check consistency")
                    .withRequiredArg().ofType(Integer.class).defaultsTo(Runtime.getRuntime().availableProcessors());
            paths = parser.accepts("path", "Limit check to given path")
                    .withRequiredArg().ofType(String.class);
        }

        @Override
        public CheckOptions parse(String[] args) {
            super.parse(args);
            return this;
        }

        public String getOutput() {
            return out.value(options);
        }

        public boolean withProgress() {
            return progress.value(options);
        }

        public boolean isSilent() {
            return options.has(silent);
        }

        public boolean withSummary() {
            return summary.value(options);
        }

        public boolean withCounter() {
            return counter.value(options);
        }

        public boolean withOrphan() {
            return orphan.value(options);
        }

        public boolean withBaseVersion() {
            return baseVersion.value(options);
        }

        public boolean withVersionHistory() {
            return versionHistory.value(options);
        }

        public boolean withPredecessors() {
            return predecessors.value(options);
        }

        public boolean withSuccessors() {
            return successors.value(options);
        }

        public boolean withUuid() {
            return uuid.value(options);
        }

        public boolean withConsistency() {
            return consistency.value(options);
        }

        public int getNumThreads() {
            return numThreads.value(options);
        }

        public List<String> getPaths() {
            return paths.values(options);
        }

        boolean isHelp() {
            return options.has(help);
        }
    }
}
