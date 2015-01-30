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
package org.jboss.aerogear.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.aerogear.sync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.jsonpatch.JsonPatchEdit;
import org.junit.Test;

public class JsonPatchClientIntegrationTest {
    
    @Test
    public void connect() throws InterruptedException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode originalVersion = objectMapper.createObjectNode().put("name", "fletch");
        final String documentId = "1234";
        final String clientId = "client2";
        final JsonPatchClientSynchronizer synchronizer = new JsonPatchClientSynchronizer();
        final ClientInMemoryDataStore<JsonNode, JsonPatchEdit> dataStore = new ClientInMemoryDataStore<JsonNode, JsonPatchEdit>();
        final ClientSyncEngine<JsonNode, JsonPatchEdit> clientSyncEngine = new ClientSyncEngine<JsonNode, JsonPatchEdit>(synchronizer, dataStore);
        final DiffSyncClient<JsonNode, JsonPatchEdit> client = DiffSyncClient.<JsonNode, JsonPatchEdit>forHost("localhost")
                .syncEngine(clientSyncEngine)
                .port(7777)
                .path("/sync")
                .build();
        client.connect();
        client.addDocument(clientDoc(documentId, clientId, originalVersion));
        Thread.sleep(1000);
        client.disconnect();
    }
    
    private static ClientDocument<JsonNode> clientDoc(final String docId, final String clientId, final JsonNode content) {
        return new DefaultClientDocument<JsonNode>(docId, clientId, content);
    }
  

}