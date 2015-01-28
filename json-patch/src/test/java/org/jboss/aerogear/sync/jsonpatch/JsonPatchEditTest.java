package org.jboss.aerogear.sync.jsonpatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.diff.JsonDiff;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonPatchEditTest {

    @Test
    public void createJsonPatchEdit() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode source = objectMapper.createObjectNode().put("name", "fletch");
        final ObjectNode target = objectMapper.createObjectNode().put("name", "Fletch");
        final JsonPatch jsonPatch = JsonDiff.asJsonPatch(source, target);
        final JsonPatchEdit edit = JsonPatchEdit.withDocumentId("1234").clientId("client1").diff(jsonPatch).build();
        assertThat(edit.diffs().isEmpty(), is(false));
    }

}