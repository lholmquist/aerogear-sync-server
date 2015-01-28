package org.jboss.aerogear.sync.jsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.diff.JsonDiff;
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
        final PatchMessage<JsonPatchEdit> patchMessage = patchMessage(documentId, clientId);

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
        final JsonNode patch = diffs.get(0).get(0);
        assertThat(patch.get("op").asText(), equalTo("replace"));
        assertThat(patch.get("path").asText(), equalTo("/name"));
        assertThat(patch.get("value").asText(), equalTo("Fletch"));
    }

    @Test
    public void patchMessageFromJson() {
        final String documentId = "1234";
        final String clientId = "client1";
        final JsonPatch patch = jsonPatch();
        final String json = JsonMapper.toJson(patchMessage(documentId, clientId, patch));
        final JsonPatchMessage patchMessage = JsonMapper.fromJson(json, JsonPatchMessage.class);
        assertThat(patchMessage.documentId(), equalTo(documentId));
        assertThat(patchMessage.clientId(), equalTo(clientId));
        assertThat(patchMessage.edits().size(), is(1));
        assertThat(patchMessage.edits().peek().diffs().size(), is(1));
        assertThat(patchMessage.edits().peek().diffs().peek().jsonPatch().toString(), equalTo(patch.toString()));
    }

    private static PatchMessage<JsonPatchEdit> patchMessage(final String documentId, final String clientId) {
        final JsonPatch jsonPatch = jsonPatch();
        return patchMessage(documentId, clientId, JsonPatchEdit.withDocumentId(documentId)
                .clientId(clientId)
                .diff(jsonPatch)
                .build());
    }

    private static PatchMessage<JsonPatchEdit> patchMessage(final String documentId,
                                                            final String clientId,
                                                            final JsonPatch jsonPatch) {
        return patchMessage(documentId, clientId, JsonPatchEdit.withDocumentId(documentId)
                .clientId(clientId)
                .diff(jsonPatch)
                .build());
    }

    private static JsonPatch jsonPatch() {
        final ObjectNode source = objectMapper.createObjectNode().put("name", "fletch");
        final ObjectNode target = objectMapper.createObjectNode().put("name", "Fletch");
        return JsonDiff.asJsonPatch(source, target);
    }

    private static PatchMessage<JsonPatchEdit> patchMessage(final String docId, final String clientId, JsonPatchEdit... edit) {
        return new JsonPatchMessage(docId, clientId, new LinkedList<JsonPatchEdit>(Arrays.asList(edit)));
    }
}
