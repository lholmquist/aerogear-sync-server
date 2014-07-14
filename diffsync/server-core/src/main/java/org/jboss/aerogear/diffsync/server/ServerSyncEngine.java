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
package org.jboss.aerogear.diffsync.server;

import org.jboss.aerogear.diffsync.*;

import java.util.Iterator;
import java.util.Queue;

/**
 * The server side of the differential synchronization implementation.
 *
 * @param <T> The type of document that this implementation can handle.
 */
public class ServerSyncEngine<T> {

    private final ServerSynchronizer<T> synchronizer;
    private final ServerDataStore<T> dataStore;

    public ServerSyncEngine(final ServerSynchronizer<T> synchronizer, final ServerDataStore<T> dataStore) {
        this.synchronizer = synchronizer;
        this.dataStore = dataStore;
    }

    /**
     * Adds a new document which is "synchonrizable".
     *
     * A server does not create a new document itself, this would be created by a client
     * and a first revision is added to this synchronization engine by this method call.
     *
     * @param document the document to add.
     */
    public void addDocument(final Document<T> document, final String clientId) {
        if (!contains(document.id())) {
            dataStore.saveDocument(document);
        }
        addShadow(document.id(), clientId);
    }

    /**
     * Performs the server side diff which is performed when the server document is modified.
     * The produced {@link Edit} can be sent to the client for patching the client side documents.
     *
     *
     * @param documentId the document in question.
     * @param clientId the clientId for whom we should perform a diff and create edits for.
     * @return {@link Edit} The server edits, or updates, that were generated by this diff .
     */
    public Edit diff(final String documentId, final String clientId) {
        final Document<T> document = getDocument(documentId);
        final Edit edit = serverDiffs(document, clientId);
        diffPatchShadow(getShadowDocument(documentId, clientId), edit);
        return edit;
    }

    /**
     * Performs the server side patching for a specific client.
     *
     * @param edits the changes made by a client.
     */
    public void patch(final Edits edits) {
        final ShadowDocument<T> shadow = getShadowDocument(edits.documentId(), edits.clientId());
        final ShadowDocument<T> patchedShadow = patchShadow(shadow, edits.edits());
        final Document<T> document = getDocument(patchedShadow.document().id());
        patchDocument(document, patchedShadow);
        saveBackupShadow(shadow);
    }

    public Edits diffs(final String documentId, final String clientId) {
        diff(documentId, clientId);
        return new DefaultEdits(documentId, clientId, dataStore.getEdits(documentId, clientId));
    }

    private boolean contains(final String id) {
        return dataStore.getDocument(id) != null;
    }

    private void diffPatchShadow(final ShadowDocument<T> shadow, final Edit edit) {
        saveShadow(synchronizer.patchShadow(edit, shadow));
    }

    private void addShadow(final String documentId, final String clientId) {
        final Document<T> document = getDocument(documentId);
        final ClientDocument<T> clientDocument = newClientDocument(documentId, clientId, document.content());
        // A clients shadow always begins with server version 0, and client version 0. Even if the server document
        // has existed for days and has been updated many time, the server version of the shadow is specific to this
        // client. The server version represents the latest version of the server document that the client has seen.
        saveShadow(newShadowDoc(0, 0, clientDocument));
    }

    private Edit clientDiffs(final Document<T> document, final ShadowDocument<T> shadow) {
        return clientDiff(document, shadow);
    }

    private Edit serverDiffs(final Document<T> document, final String clientId) {
        final ShadowDocument<T> shadow = getShadowDocument(document.id(), clientId);
        final Edit newEdit = serverDiff(document, shadow);
        saveEdits(newEdit);
        saveShadow(incrementServerVersion(shadow));
        return newEdit;
    }

    private ShadowDocument<T> patchShadow(final ShadowDocument<T> shadowDocument, final Queue<Edit> edits) {
        ShadowDocument<T> shadow = copy(shadowDocument);

        final Iterator<Edit> iterator = edits.iterator();
        while (iterator.hasNext()) {
            final Edit edit = iterator.next();
            if (edit.serverVersion() < shadow.serverVersion()) {
                final BackupShadowDocument<T> backupShadow = getBackupShadowDocument(edit.documentId(), edit.clientId());
                if (backupShadow.version() == edit.serverVersion()) {
                    shadow = saveShadow(newShadowDoc(backupShadow.version(), shadow.clientVersion(), backupShadow.shadow().document()));
                } else {
                    throw new IllegalStateException(backupShadow + " server version does not match version of " + edit.serverVersion());
                }
            }
            // the server has already seen this version from the client. Possibly because a packet has been
            // dropped when sending from the server to the client. We don't need to apply it and can safely
            // drop it and process the next edit.
            if (edit.clientVersion() < shadow.clientVersion()) {
                dataStore.removeEdit(edit);
                iterator.remove();
                continue;
            }
            if (edit.serverVersion() == shadow.serverVersion() && edit.clientVersion() == shadow.clientVersion()) {
                final ShadowDocument<T> patchedShadow = synchronizer.patchShadow(edit, shadow);
                dataStore.removeEdit(edit);
                shadow = saveShadow(incrementClientVersion(patchedShadow));
            }
        }
        return shadow;
    }

    private Document<T> patchDocument(final Document<T> document, final ShadowDocument<T> shadowDocument) {
        final Edit edit = clientDiffs(document, shadowDocument);
        final Document<T> patched = synchronizer.patchDocument(edit, document);
        saveDocument(patched);
        return patched;
    }

    private Document<T> getDocument(final String documentId) {
        return dataStore.getDocument(documentId);
    }

    private ClientDocument<T> newClientDocument(final String documentId, final String clientId, final T content) {
        return new DefaultClientDocument<T>(documentId, content, clientId);
    }

    private ShadowDocument<T> getShadowDocument(final String documentId, final String clientId) {
        return dataStore.getShadowDocument(documentId, clientId);
    }

    private BackupShadowDocument<T> getBackupShadowDocument(final String documentId, final String clientId) {
        return dataStore.getBackupShadowDocument(documentId, clientId);
    }

    private Edit clientDiff(final Document<T> doc, final ShadowDocument<T> shadow) {
        return synchronizer.clientDiff(doc, shadow);
    }

    private Edit serverDiff(final Document<T> doc, final ShadowDocument<T> shadow) {
        return synchronizer.serverDiff(doc, shadow);
    }

    private void saveEdits(final Edit edit) {
        dataStore.saveEdits(edit);
    }

    private ShadowDocument<T> incrementClientVersion(final ShadowDocument<T> shadow) {
        final long clientVersion = shadow.clientVersion() + 1;
        return newShadowDoc(shadow.serverVersion(), clientVersion, shadow.document());
    }

    private ShadowDocument<T> saveShadow(final ShadowDocument<T> newShadow) {
        dataStore.saveShadowDocument(newShadow);
        return newShadow;
    }

    private ShadowDocument<T> newShadowDoc(final long serverVersion, final long clientVersion, final ClientDocument<T> doc) {
        return new DefaultShadowDocument<T>(serverVersion, clientVersion, doc);
    }

    private ShadowDocument<T> copy(final ShadowDocument<T> shadow) {
        return new DefaultShadowDocument<T>(shadow.serverVersion(), shadow.clientVersion(), shadow.document());
    }

    private ShadowDocument<T> incrementServerVersion(final ShadowDocument<T> shadow) {
        final long serverVersion = shadow.serverVersion() + 1;
        return newShadowDoc(serverVersion, shadow.clientVersion(), shadow.document());
    }

    private void saveBackupShadow(final ShadowDocument<T> newShadow) {
        dataStore.saveBackupShadowDocument(new DefaultBackupShadowDocument<T>(newShadow.clientVersion(), newShadow));
    }

    private void saveDocument(final Document<T> document) {
        dataStore.saveDocument(document);
    }

}
