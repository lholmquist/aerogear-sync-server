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

import java.util.Queue;

/**
 * A DataStore implementation is responible for storing and serving data for a
 * Differential Synchronization implementation.
 *
 * @param <T> The type of the Document that this data store can handle.
 */
public interface DataStore<T, S extends Edit> {

    /**
     * Saves a shadow document.
     *
     * @param shadowDocument the {@link ShadowDocument} to save.
     */
    void saveShadowDocument(ShadowDocument<T> shadowDocument);

    /**
     * Retrieves the {@link ShadowDocument} matching the passed-in document documentId.
     *
     * @param documentId the document id of the shadow document.
     * @param clientId the client for which to retrieve the shadow document.
     * @return {@link ShadowDocument} the shadow document matching the documentId.
     */
    ShadowDocument<T> getShadowDocument(String documentId, String clientId);

    /**
     * Saves a backup shadow document
     *
     * @param backupShadow the {@link BackupShadowDocument} to save.
     */
    void saveBackupShadowDocument(BackupShadowDocument<T> backupShadow);

    /**
     * Retrieves the {@link BackupShadowDocument} matching the passed-in document documentId.
     *
     * @param documentId the document identifier of the backup shadow document.
     * @param clientId the client identifier for which to fetch the document.
     * @return {@link BackupShadowDocument} the backup shadow document matching the documentId.
     */
    BackupShadowDocument<T> getBackupShadowDocument(String documentId, String clientId);

    /**
     * Saves an {@link Edit} to the data store.
     *
     * @param edit the edit to be saved.
     */
    void saveEdits(S edit);

    /**
     * Retreives the queue of {@link Edit}s for the specified document documentId.
     *
     * @param documentId the document identifier of the edit.
     * @param clientId the client identifier for which to fetch the document.
     * @return {@code Queue<S>} the edits for the document.
     */
    Queue<S> getEdits(String documentId, String clientId);

    /**
     * Removes the edit from the store.
     *
     * @param edit the edit to be removed.
     */
    void removeEdit(S edit);

    /**
     * Removes all edits for the specific client and document pair.
     *
     * @param documentId the document identifier of the edit.
     * @param clientId the client identifier.
     */
    void removeEdits(String documentId, String clientId);

}
