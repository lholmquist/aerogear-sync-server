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
package org.jboss.aerogear.sync.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.aerogear.sync.ClientDocument;
import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.Edit;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.ShadowDocument;

import java.util.Queue;

/**
 * An instance of this class will be able to handle tasks needed to implement
 * Differential Synchronization for a specific type of documents.
 *
 * @param <T> The type of documents that this synchronizer can handle
 * @param <S> The type of {@link Edit}s that this synchronizer can handle
 */
public interface ClientSynchronizer<T, S extends Edit<? extends Diff>> {

    /**
     * Called when the shadow should be patched. Is called when an update is recieved.
     *
     * @param edit the {@link Edit} containing the diffs/patches
     * @param shadowDocument the {@link ShadowDocument} to be patched
     * @return {@link ShadowDocument} a new patched shadow document
     */
    ShadowDocument<T> patchShadow(S edit, ShadowDocument<T> shadowDocument);

    /**
     * Called when the document should be patched.
     *
     * @param edit the {@link Edit} containing the diffs/patches
     * @param document the {@link ClientDocument} to be patched
     * @return {@link ClientDocument} a new patched document.
     */
    ClientDocument<T> patchDocument(S edit, ClientDocument<T> document);

    /**
     * Produces a {@link Edit} containing the changes between the updated {@link ClientDocument}
     * and the {@link ShadowDocument}.
     * <p>
     * Calling the method is the first step in when starting a client side synchronization. We need to
     * gather the changes between the updates made by the client and the shadow document.
     * The produced {@code Edit} can then be passed to the server side.
     *
     * @param document the {@link ClientDocument} containing updates made by the client
     * @param shadowDocument the {@link ShadowDocument} for the {@code ClientDocument}
     * @return {@link Edit} the edit representing the diff between the client document and it's shadow document.
     */
    S serverDiff(ClientDocument<T> document, ShadowDocument<T> shadowDocument);
    
    /**
     * Produces a {@link Edit} containing the changes between updated {@link ShadowDocument}
     * and the {@link ClientDocument}.
     * This method would be called when the client receives an update from the server and need
     * to produce an {@code Edit} to be able to patch the {@code ClientDocument}.
     *
     * @param shadowDocument the {@link ShadowDocument} patched with updates from the server
     * @param document the {@link ClientDocument}
     * @return {@link Edit} the edit representing the diff between the shadow document and the client document.
     */
    S clientDiff(ShadowDocument<T> shadowDocument, ClientDocument<T> document);

    /**
     * Creates a new {@link PatchMessage} with the with the type of {@link Edit} that this
     * synchronizer can handle.
     *
     * @param documentId the document identifier for the {@code PatchMessage}
     * @param clientId the client identifier for the {@code PatchMessage}
     * @param edits the {@link Edit}s for the {@code PatchMessage}
     * @return {@link PatchMessage} the created {code PatchMessage}
     */
    PatchMessage<S> createPatchMessage(String documentId, String clientId, Queue<S> edits);

    /**
     * Creates a {link PatchMessage} by parsing the passed-in json.
     *
     * @param json the json representation of a {@code PatchMessage}
     * @return {@link PatchMessage} the created {code PatchMessage}
     */
    PatchMessage<S> patchMessageFromJson(String json);

    /**
     * Adds the content of the passed in {@code content} to the {@link ObjectNode}.
     * <p>
     * When a client initially adds a document to the engine it will also be sent across the
     * wire to the server. Before sending, the content of the document has to be added to the
     * JSON message payload. Different implementation will require different content types that
     * the engine can handle and this give them control over how the content is added to the JSON
     * {@code ObjectNode}.
     * For example, a ClientEngine that stores simple text will just add the contents as a String,
     * but one that stores JsonNode object will want to add its content as an object.
     *
     * @param content the content to be added
     * @param fieldName the name of the field
     */
    void addContent(T content, ObjectNode objectNode, String fieldName);

}
