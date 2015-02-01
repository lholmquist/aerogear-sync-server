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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.jboss.aerogear.sync.PatchMessage;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonMapperTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void patchMessageToJson() {
        final String documentId = "1234";
        final String clientId = "client1";
        final PatchMessage<JsonMergePatchEdit> patchMessage = patchMessage(documentId, clientId);

        final String json = JsonMapper.toJson(patchMessage);
        final JsonNode jsonNode = JsonMapper.asJsonNode(json);
        assertThat(jsonNode.get("msgType").asText(), equalTo("patch"));
        assertThat(jsonNode.get("id").asText(), equalTo(documentId));
        assertThat(jsonNode.get("clientId").asText(), equalTo(clientId));
        final JsonNode editsNode = jsonNode.get("edits");
        assertThat(editsNode.isArray(), is(true));
        assertThat(editsNode.size(), is(1));
        final JsonNode edit = editsNode.iterator().next();
        assertThat(edit.get("serverVersion").asText(), equalTo("0"));
        assertThat(edit.get("clientVersion").asText(), equalTo("0"));
        final JsonNode diffs = edit.get("diffs");
        assertThat(diffs.isArray(), is(true));
        final JsonNode patch = diffs.get(0);
        assertThat(patch.get("name").asText(), equalTo("Fletch"));
    }

    @Test
    public void patchMessageFromJson() throws JsonPatchException {
        final String documentId = "1234";
        final String clientId = "client1";
        final ObjectNode original = objectMapper.createObjectNode().put("name", "fletch");
        final String json = JsonMapper.toJson(patchMessage(documentId, clientId, jsonMergePatch()));
        final JsonMergePatchMessage patchMessage = JsonMapper.fromJson(json, JsonMergePatchMessage.class);
        assertThat(patchMessage.documentId(), equalTo(documentId));
        assertThat(patchMessage.clientId(), equalTo(clientId));
        assertThat(patchMessage.edits().size(), is(1));
        assertThat(patchMessage.edits().peek().diffs().size(), is(1));
        final JsonMergePatch patch = patchMessage.edits().peek().diffs().peek().jsonMergePatch();
        final JsonNode patched = patch.apply(original);
        assertThat(patched.get("name").asText(), equalTo("Fletch"));
    }

    @Test
    public void jsonPatchEditToJson() {
        final String documentId = "1234";
        final String clientId = "client1";
        final String json = JsonMapper.toJson(jsonPatchEdit(documentId, clientId, jsonMergePatch()));
        final JsonNode edit = JsonMapper.asJsonNode(json);
        assertThat(edit.get("serverVersion").asText(), equalTo("0"));
        assertThat(edit.get("clientVersion").asText(), equalTo("0"));
        final JsonNode diffs = edit.get("diffs");
        assertThat(diffs.isArray(), is(true));
        final JsonNode patch = diffs.get(0);
        assertThat(patch.get("name").asText(), equalTo("Fletch"));
    }

    @Test
    public void jsonPatchEditFromJson() throws JsonPatchException {
        final String documentId = "1234";
        final String clientId = "client1";
        final ObjectNode original = objectMapper.createObjectNode().put("name", "fletch");
        final String json = JsonMapper.toJson(jsonPatchEdit(documentId, clientId, jsonMergePatch()));
        final JsonMergePatchEdit edit = JsonMapper.fromJson(json, JsonMergePatchEdit.class);
        assertThat(edit.documentId(), equalTo(documentId));
        assertThat(edit.clientId(), equalTo(clientId));
        assertThat(edit.diffs().size(), is(1));
        final JsonMergePatch patch = edit.diffs().get(0).jsonMergePatch();
        final JsonNode patched = patch.apply(original);
        assertThat(patched.get("name").asText(), equalTo("Fletch"));
    }

    private static PatchMessage<JsonMergePatchEdit> patchMessage(final String documentId, final String clientId) {
        final JsonMergePatch jsonPatch = jsonMergePatch();
        return patchMessage(documentId, clientId, jsonPatchEdit(documentId, clientId, jsonPatch));
    }

    private static JsonMergePatchEdit jsonPatchEdit(final String documentId, final String clientId, final JsonMergePatch patch) {
        return JsonMergePatchEdit.withDocumentId(documentId)
                .clientId(clientId)
                .diff(patch)
                .build();
    }

    private static PatchMessage<JsonMergePatchEdit> patchMessage(final String documentId,
                                                            final String clientId,
                                                            final JsonMergePatch jsonMergePatch) {
        return patchMessage(documentId, clientId, JsonMergePatchEdit.withDocumentId(documentId)
                .clientId(clientId)
                .diff(jsonMergePatch)
                .build());
    }

    private static JsonMergePatch jsonMergePatch() {
        try {
            final ObjectNode mergePatch = objectMapper.createObjectNode().put("name", "Fletch");
            return JsonMergePatch.fromJson(mergePatch);
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static PatchMessage<JsonMergePatchEdit> patchMessage(final String docId, final String clientId, JsonMergePatchEdit... edit) {
        return new JsonMergePatchMessage(docId, clientId, new LinkedList<JsonMergePatchEdit>(Arrays.asList(edit)));
    }
}
