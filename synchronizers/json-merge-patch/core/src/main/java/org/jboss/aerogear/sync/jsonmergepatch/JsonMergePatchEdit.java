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
package org.jboss.aerogear.sync.jsonmergepatch;

import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.jboss.aerogear.sync.Edit;

public class JsonMergePatchEdit implements Edit<JsonMergePatchDiff> {

    private final long clientVersion;
    private final long serverVersion;
    private final String checksum;
    private final JsonMergePatchDiff diff;

    private JsonMergePatchEdit(final Builder builder) {
        clientVersion = builder.clientVersion;
        serverVersion = builder.serverVersion;
        checksum = builder.checksum;
        diff = builder.diff;
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
    public JsonMergePatchDiff diff() {
        return diff;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JsonMergePatchEdit that = (JsonMergePatchEdit) o;

        if (clientVersion != that.clientVersion) {
            return false;
        }
        if (serverVersion != that.serverVersion) {
            return false;
        }
        if (!diff.equals(that.diff)) {
            return false;
        }
        return !checksum.equals(that.checksum);
    }

    @Override
    public int hashCode() {
        int result = checksum.hashCode();
        result = 31 * result + (int) (clientVersion ^ clientVersion >>> 32);
        result = 31 * result + (int) (serverVersion ^ serverVersion >>> 32);
        result = 31 * result + diff.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "JsonPatctEdit[serverVersion=" + serverVersion +
                ", clientVersion=" + clientVersion +
                ", diffs=" + diff + ']';
    }

    public static Builder withPatch(final JsonMergePatch patch) {
        return new Builder(patch);
    }

    public static Builder withChecksum(final String checksum) {
        return new Builder(checksum);
    }

    public static class Builder {

        private long serverVersion;
        private long clientVersion;
        private String checksum;
        private JsonMergePatchDiff diff;

        private Builder(final JsonMergePatch patch) {
            diff = new JsonMergePatchDiff(patch);
        }

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

        public Builder checksum(final String checksum) {
            this.checksum = checksum;
            return this;
        }

        public Builder patch(final JsonMergePatch patch) {
            diff = new JsonMergePatchDiff(patch);
            return this;
        }

        public JsonMergePatchEdit build() {
            return new JsonMergePatchEdit(this);
        }
    }
}
