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
package org.jboss.aerogear.sync.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.Edit;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.ShadowDocument;

import java.util.Queue;

/**
 * A instance of this class will be able to handle tasks needed to implement
 * Differential Synchronization for a specific type of documents.
 *
 * @param <T> The type of documents that this engine can handle.
 */
public interface ServerSynchronizer<T, S extends Edit> {

    /**
     * Called when the shadow should be patched. Is called when an update is recieved.
     *
     * @param edit The edits.
     * @return {@link ShadowDocument} a new patched shadow document.
     */
    ShadowDocument<T> patchShadow(S edit, ShadowDocument<T> shadowDocument);

    /**
     * Called when the document should be patched.
     *
     * @param edit the edit to use to path the document
     * @param document the document to be patched.
     * @return {@link Document} a new patched document.
     */
    Document<T> patchDocument(S edit, Document<T> document);

    /**
     * The first step in a sync is to produce a an edit for the changes.
     * The produced edit can then be sent to the opposing side perform an update/sync.
     *
     * @param document the document containing
     * @param shadowDocument the document shadow.
     * @return {@link Edit} the edit representing the diff between the document and it's shadow document.
     */
    S serverDiff(Document<T> document, ShadowDocument<T> shadowDocument);

    /**
     * Is called to produce an {@link Edit} of changes coming from a client.
     *
     * @param document the server side document .
     * @param shadowDocument the document shadow containing the client changes.
     * @return {@link Edit} the edit representing the diff between the document and it's shadow document.
     */
    S clientDiff(Document<T> document, ShadowDocument<T> shadowDocument);

    PatchMessage<S> createPatchMessage(String documentId, String clientId, Queue<S> edits);

    PatchMessage<S> patchMessageFromJson(String json);
    String patchMessageToJson(PatchMessage<S> patchMessage);

    Document<T> documentFromJson(JsonNode json);

}
