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
package org.jboss.aerogear.sync.jsonpatch;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jboss.aerogear.sync.jsonpatch.Patches.*;

public class JsonPatchEditTest {

    @Test (expected = NullPointerException.class)
    public void constructWithNullPatch() {
        JsonPatchEdit.withPatch(null).build();
    }

    @Test (expected = NullPointerException.class)
    public void constructWithNullChecksum() {
        JsonPatchEdit.withPatch(jsonPatch()).checksum(null).build();
    }

    @Test
    public void equalsReflexsive() throws Exception {
        final JsonPatchEdit x = newJsonPatchEdit();
        assertThat(x, equalTo(x));
    }

    @Test
    public void equalsSymmetric() throws Exception {
        final JsonPatchEdit x = newJsonPatchEdit();
        final JsonPatchEdit y = newJsonPatchEdit();
        assertThat(x, equalTo(y));
        assertThat(y, equalTo(x));
    }

    @Test
    public void equalsTransitive() {
        final JsonPatchEdit x = newJsonPatchEdit();
        final JsonPatchEdit y = newJsonPatchEdit();
        final JsonPatchEdit z = newJsonPatchEdit();
        assertThat(x, equalTo(y));
        assertThat(y, equalTo(z));
        assertThat(x, equalTo(z));
    }

    @Test
    public void equalsNull() {
        final JsonPatchEdit x = newJsonPatchEdit();
        assertThat(x.equals(null), is(false));
    }

    @Test
    public void nonEquals() {
        final JsonPatchEdit x = newJsonPatchEdit();
        final JsonPatchEdit y = JsonPatchEdit.withPatch(jsonPatch("lisa", "Lisa")).checksum("123").build();
        assertThat(x.equals(y), is(false));
    }

}