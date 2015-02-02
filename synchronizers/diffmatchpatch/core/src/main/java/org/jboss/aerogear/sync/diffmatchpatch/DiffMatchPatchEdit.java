/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.sync.diffmatchpatch;

import org.jboss.aerogear.sync.Edit;

import java.util.LinkedList;

import static org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff.Operation;

public class DiffMatchPatchEdit implements Edit<DiffMatchPatchDiffs> {

    private final long clientVersion;
    private final long serverVersion;
    private final String checksum;
    private final DiffMatchPatchDiffs diffs;

    private DiffMatchPatchEdit(final Builder builder) {
        clientVersion = builder.clientVersion;
        serverVersion = builder.serverVersion;
        checksum = builder.checksum;
        diffs = new DiffMatchPatchDiffs(builder.diffs);
    }

    @Override
    public long clientVersion() {
        return clientVersion;
    }

    @Override
    public long serverVersion() {
        return serverVersion;
    }

    @Override
    public String checksum() {
        return checksum;
    }

    @Override
    public DiffMatchPatchDiffs diff() {
        return diffs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DiffMatchPatchEdit that = (DiffMatchPatchEdit) o;

        if (clientVersion != that.clientVersion) {
            return false;
        }
        if (serverVersion != that.serverVersion) {
            return false;
        }
        if (!diffs.equals(that.diffs)) {
            return false;
        }
        return !checksum.equals(that.checksum);
    }

    @Override
    public int hashCode() {
        int result = checksum.hashCode();
        result = 31 * result + (int) (clientVersion ^ clientVersion >>> 32);
        result = 31 * result + (int) (serverVersion ^ serverVersion >>> 32);
        result = 31 * result + checksum.hashCode();
        result = 31 * result + diffs.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DefaultEdit[serverVersion=" + serverVersion +
                ", clientVersion=" + clientVersion +
                ", diffs=" + diffs + ']';
    }

    public static Builder withChecksum(final String checksum) {
        return new Builder(checksum);
    }

    public static class Builder {

        private long serverVersion;
        private long clientVersion;
        private final String checksum;
        private final LinkedList<DiffMatchPatchDiff> diffs = new LinkedList<DiffMatchPatchDiff>();

        private Builder(final String checksum) {
            this.checksum = checksum;
        }

        public Builder serverVersion(final long serverVersion) {
            this.serverVersion = serverVersion;
            return this;
        }

        public Builder clientVersion(final long clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        public Builder unchanged(final String text) {
            diffs.add(new DiffMatchPatchDiff(Operation.UNCHANGED, text));
            return this;
        }

        public Builder add(final String text) {
            diffs.add(new DiffMatchPatchDiff(Operation.ADD, text));
            return this;
        }

        public Builder delete(final String text) {
            diffs.add(new DiffMatchPatchDiff(Operation.DELETE, text));
            return this;
        }

        public Builder diff(final DiffMatchPatchDiff diff) {
            diffs.add(diff);
            return this;
        }

        public Builder diffs(final LinkedList<DiffMatchPatchDiff> diffs) {
            this.diffs.addAll(diffs);
            return this;
        }

        public DiffMatchPatchEdit build() {
            return new DiffMatchPatchEdit(this);
        }
    }
}
