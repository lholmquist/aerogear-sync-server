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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.jboss.aerogear.sync.jsonmergepatch.JsonMergePatchEdit;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonMergePatchEditTest {

    @Test
    public void createJsonPatchEdit() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode mergePatch = objectMapper.createObjectNode().put("name", "Fletch");
        final JsonMergePatch jsonMergePatch = JsonMergePatch.fromJson(mergePatch);
        final JsonMergePatchEdit edit = JsonMergePatchEdit.withDocumentId("1234")
                .clientId("client1")
                .diff(jsonMergePatch).build();
        assertThat(edit.diff(), is(notNullValue()));
        assertThat(edit.diff().jsonMergePatch(), equalTo(jsonMergePatch));
    }


}