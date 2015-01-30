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

import org.jboss.aerogear.sync.DataStore;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.Edit;

/**
 * A server side {@link DataStore} implementation is responsible for storing and serving data for a
 * Differential Synchronization implementation.
 *
 * @param <T> The type of the Document that this data store can handle.
 * @param <S> The type of {@link Edit}s that this synchronizer can handle
 */
public interface ServerDataStore<T, S extends Edit> extends DataStore<T, S> {

    /**
     * Saves a server document.
     *
     * @param document the {@link Document} to save.
     * @return {code boolean} true if the document was stored to the underlying store.
     */
    boolean saveDocument(Document<T> document);

    /**
     * Updates a server document.
     *
     * @param document the {@link Document} to update.
     */
    void updateDocument(Document<T> document);

    /**
     * Retrieves the {@link Document} matching the passed-in document documentId.
     *
     * @param documentId the document identifier of the shadow document.
     * @return {@link Document} the document matching the documentId.
     */
    Document<T> getDocument(String documentId);

}
