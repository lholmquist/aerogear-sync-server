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
package org.jboss.aerogear.sync.jsonpatch.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.aerogear.sync.DefaultDocument;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.jsonpatch.JsonPatchEdit;
import org.jboss.aerogear.sync.server.ServerInMemoryDataStore;
import org.junit.Test;

import java.util.Iterator;
import java.util.Queue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonPatchInMemoryDataStoreTest {

    private static final ObjectMapper OM = new ObjectMapper();

    @Test
    public void getEdits() {
        final String documentId = "12345";
        final String clientId = "client1";
        final ServerInMemoryDataStore<JsonNode, JsonPatchEdit> dataStore = new ServerInMemoryDataStore<JsonNode, JsonPatchEdit>();
        final JsonPatchEdit editOne = JsonPatchEdit.withDocumentId(documentId).clientId(clientId).clientVersion(0).build();
        final JsonPatchEdit editTwo = JsonPatchEdit.withDocumentId(documentId).clientId(clientId).clientVersion(1).build();
        dataStore.saveEdits(editOne);
        dataStore.saveEdits(editTwo);
        final Queue<JsonPatchEdit> edits = dataStore.getEdits(documentId, clientId);
        assertThat(edits.size(), is(2));
        final Iterator<JsonPatchEdit> iterator = edits.iterator();
        assertThat(iterator.next().clientVersion(), is(0L));
        assertThat(iterator.next().clientVersion(), is(1L));
    }

    @Test
    public void saveDocument() {
        final String documentId = "1234";
        final JsonNode content = OM.createObjectNode().put("name", "fletch");
        final ServerInMemoryDataStore<JsonNode, JsonPatchEdit> dataStore = new ServerInMemoryDataStore<JsonNode, JsonPatchEdit>();
        dataStore.saveDocument(new DefaultDocument<JsonNode>(documentId, content));
        final Document<JsonNode> document = dataStore.getDocument(documentId);
        assertThat(document.content(), equalTo(content));
    }
}
