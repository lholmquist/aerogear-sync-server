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
package org.jboss.aerogear.sync.jsonmergepatch.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.jboss.aerogear.sync.DefaultClientDocument;
import org.jboss.aerogear.sync.DefaultDocument;
import org.jboss.aerogear.sync.DefaultShadowDocument;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.ShadowDocument;
import org.jboss.aerogear.sync.jsonmergepatch.JsonMergePatchDiff;
import org.jboss.aerogear.sync.jsonmergepatch.JsonMergePatchEdit;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonMergePatchServerSynchronizerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonMergePatchServerSynchronizer syncer = new JsonMergePatchServerSynchronizer();

    @Test
    public void clientDiff() throws Exception {
        final String documentId = "1234";
        final String clientId = "client1";
        final ObjectNode source = objectMapper.createObjectNode().put("name", "fletch");
        final ObjectNode updated = objectMapper.createObjectNode().put("name", "Fletch");
        final DefaultShadowDocument<JsonNode> shadowDocument = new DefaultShadowDocument<JsonNode>(0, 0,
                new DefaultClientDocument<JsonNode>(documentId, clientId, updated));
        final DefaultDocument<JsonNode> document = new DefaultDocument<JsonNode>(documentId, source);
        final JsonMergePatchEdit jsonPatchEdit = syncer.clientDiff(document, shadowDocument);
        assertThat(jsonPatchEdit.diff(), is(notNullValue()));
        final JsonMergePatch patch = jsonPatchEdit.diff().jsonMergePatch();
        final JsonNode patched = patch.apply(source);
        assertThat(patched.get("name").asText(), equalTo("Fletch"));
    }

    @Test
    public void serverDiff() throws Exception {
        final String documentId = "1234";
        final String clientId = "client1";
        final ObjectNode source = objectMapper.createObjectNode().put("name", "fletch");
        final ObjectNode updated = objectMapper.createObjectNode().put("name", "Fletch");
        final DefaultDocument<JsonNode> document = new DefaultDocument<JsonNode>(documentId, updated);
        final DefaultShadowDocument<JsonNode> shadowDocument = new DefaultShadowDocument<JsonNode>(0, 0,
                new DefaultClientDocument<JsonNode>(documentId, clientId, source));
        final JsonMergePatchEdit jsonPatchEdit = syncer.serverDiff(document, shadowDocument);
        assertThat(jsonPatchEdit.diff(), is(notNullValue()));
        final JsonMergePatch patch = jsonPatchEdit.diff().jsonMergePatch();
        final JsonNode patched = patch.apply(source);
        assertThat(patched.get("name").asText(), equalTo("Fletch"));
    }

    @Test
    public void createPatchMessage() throws JsonPatchException {
        final String documentId = "1234";
        final String clientId = "client1";
        final ObjectNode source = objectMapper.createObjectNode().put("name", "fletch");
        final PatchMessage<JsonMergePatchEdit> patchMessage = syncer.createPatchMessage(documentId, clientId,
                asQueue(jsonMergePatchEdit(jsonMergePatch())));
        assertThat(patchMessage.documentId(), equalTo(documentId));
        assertThat(patchMessage.clientId(), equalTo(clientId));
        assertThat(patchMessage.edits().size(), is(1));
        assertThat(patchMessage.edits().peek().diff(), is(notNullValue()));
        final JsonMergePatchDiff jsonMergePatchDiff = patchMessage.edits().peek().diff();
        final JsonNode patched = jsonMergePatchDiff.jsonMergePatch().apply(source);
        assertThat(patched.get("name").asText(), equalTo("Fletch"));
    }

    @Test
    public void patchDocument() {
        final String documentId = "1234";
        final ObjectNode source = objectMapper.createObjectNode().put("name", "fletch");
        final DefaultDocument<JsonNode> document = new DefaultDocument<JsonNode>(documentId, source);
        final JsonMergePatch patch = jsonMergePatch();
        final JsonMergePatchEdit edit = jsonMergePatchEdit(patch);
        final Document<JsonNode> patched = syncer.patchDocument(edit, document);
        assertThat(patched.content().get("name").asText(), equalTo("Fletch"));
    }

    @Test
    public void patchShadow() {
        final String documentId = "1234";
        final String clientId = "client1";
        final ObjectNode source = objectMapper.createObjectNode().put("name", "fletch");
        final DefaultShadowDocument<JsonNode> shadowDocument = new DefaultShadowDocument<JsonNode>(0, 0,
                new DefaultClientDocument<JsonNode>(documentId, clientId, source));
        final JsonMergePatch patch = jsonMergePatch();
        final JsonMergePatchEdit edit = jsonMergePatchEdit(patch);
        final ShadowDocument<JsonNode> patched = syncer.patchShadow(edit, shadowDocument);
        assertThat(patched.document().content().get("name").asText(), equalTo("Fletch"));
    }

    private static JsonMergePatchEdit jsonMergePatchEdit(final JsonMergePatch patch) {
        return JsonMergePatchEdit.withPatch(patch).build();
    }

    private static JsonMergePatch jsonMergePatch() {
        final ObjectNode target = objectMapper.createObjectNode().put("name", "Fletch");
        try {
            return JsonMergePatch.fromJson(target);
        } catch (final JsonPatchException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static Queue<JsonMergePatchEdit> asQueue(final JsonMergePatchEdit... edits){
        return new LinkedList<JsonMergePatchEdit>(Arrays.asList(edits));
    }

}