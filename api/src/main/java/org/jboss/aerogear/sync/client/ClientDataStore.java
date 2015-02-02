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

import org.jboss.aerogear.sync.ClientDocument;
import org.jboss.aerogear.sync.DataStore;
import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.Edit;

/**
 * A client side {@link DataStore} implementation is responsible for storing and serving data for a
 * Differential Synchronization implementation.
 *
 * @param <T> The type of the Document that this data store can handle.
 * @param <S> The type of {@link Edit}s that this synchronizer can handle
 */
public interface ClientDataStore<T, S extends Edit<? extends Diff>> extends DataStore<T, S> {

    /**
     * Saves a client document.
     *
     * @param document the {@link ClientDocument} to save.
     */
    void saveClientDocument(ClientDocument<T> document);

    /**
     * Retrieves the {@link Document} matching the passed-in document documentId.
     *
     * @param documentId the document identifier of the document.
     * @param clientId the client identifier for which to fetch the document.
     * @return {@link ClientDocument} the document matching the documentId.
     */
    ClientDocument<T> getClientDocument(String documentId, String clientId);

}
