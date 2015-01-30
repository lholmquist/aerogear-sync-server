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

import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchEdit;
import org.junit.Test;

public class DiffSyncClientIntegrationTest {
    
    @Test
    public void connect() throws InterruptedException {
        final String documentId = "1234";
        final String clientId = "client2";
        final String originalVersion = "{\"id\": 9999}";
        
        final DiffSyncClient<String, DiffMatchPatchEdit> client = DiffSyncClient.<String, DiffMatchPatchEdit>forHost("localhost").port(7777).path("/sync").build();
        client.connect();
        client.addDocument(clientDoc(documentId, clientId, originalVersion));
        Thread.sleep(1000);
        client.disconnect();
    }
    
    private static ClientDocument<String> clientDoc(final String docId, final String clientId, final String content) {
        return new DefaultClientDocument<String>(docId, clientId, content);
    }
  

}