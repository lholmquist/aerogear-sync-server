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

import org.jboss.aerogear.sync.ClientDocument;
import org.jboss.aerogear.sync.DefaultClientDocument;
import org.jboss.aerogear.sync.DefaultDocument;
import org.jboss.aerogear.sync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchEdit;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.ShadowDocument;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff.Operation;
import org.jboss.aerogear.sync.client.ClientDataStore;
import org.jboss.aerogear.sync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.diffmatchpatch.client.DefaultClientSynchronizer;
import org.jboss.aerogear.sync.diffmatchpatch.server.DiffMatchPatchServerSynchronizer;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServerSyncEngineIntegrationTest {

    private ServerDataStore<String, DiffMatchPatchEdit> dataStore;
    private ServerSyncEngine<String, DiffMatchPatchEdit> serverSyncEngine;

    @Before
    public void setup() {
        dataStore = new ServerInMemoryDataStore<String, DiffMatchPatchEdit>();
        serverSyncEngine = new ServerSyncEngine<String, DiffMatchPatchEdit>(new DiffMatchPatchServerSynchronizer(), dataStore);
    }

    @Test
    public void addDocument() {
        final String documentId = UUID.randomUUID().toString();
        serverSyncEngine.addSubscriber(new MockSubscriber("client1"), newDoc(documentId, "What!"));
        final Document<String> document = dataStore.getDocument(documentId);
        assertThat(document.id(), equalTo(documentId));
        assertThat(document.content(), equalTo("What!"));
    }

    @Test
    public void containsDocument() {
        final String documentId = UUID.randomUUID().toString();
        serverSyncEngine.addSubscriber(new MockSubscriber("client1"), newDoc(documentId, "What!"));
        assertThat(dataStore.getDocument(documentId), is(notNullValue()));
    }

    @Test
    public void containsDocumentNonExistent() {
        assertThat(dataStore.getDocument("bogusId"), is(nullValue()));
    }

    @Test
    public void addShadow() {
        final String documentId = UUID.randomUUID().toString();
        final String clientId = "shadowTest";
        serverSyncEngine.addSubscriber(new MockSubscriber(clientId), newDoc(documentId, "What!"));
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(documentId, clientId);
        assertThat(shadowDocument.document().clientId(), is("shadowTest"));
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.document().id(), equalTo(documentId));
    }

    @Test
    public void patchOneVersion() {
        final String documentId = UUID.randomUUID().toString();
        final String clientOne = "client1";
        final String clientTwo = "client2";
        final String originalVersion = "Do or do not, there is no try.";
        final String versionOne = "Do or do not, there is no try!";

        final DefaultDocument<String> serverDocument = newDoc(documentId, originalVersion);
        serverSyncEngine.addSubscriber(new MockSubscriber(clientOne), serverDocument);
        serverSyncEngine.addSubscriber(new MockSubscriber(clientTwo), serverDocument);
        serverSyncEngine.patch(clientSideEdits(documentId, originalVersion, clientOne, versionOne));

        final DiffMatchPatchEdit edit = serverSyncEngine.diff(documentId, clientTwo);
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        assertThat(edit.clientId(), equalTo(clientTwo));
        final LinkedList<DiffMatchPatchDiff> diffs = edit.diffs();
        assertThat(diffs.size(), is(3));
        assertThat(diffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(diffs.get(1).operation(), is(Operation.DELETE));
        assertThat(diffs.get(1).text(), equalTo("."));
        assertThat(diffs.get(2).operation(), is(Operation.ADD));
        assertThat(diffs.get(2).text(), equalTo("!"));

        final ShadowDocument<String> shadowAfter = dataStore.getShadowDocument(documentId, clientOne);
        assertThat(shadowAfter.clientVersion(), is(1L));
        assertThat(shadowAfter.serverVersion(), is(0L));
    }

    @Test
    public void patchTwoVersions() {
        final String documentId = UUID.randomUUID().toString();
        final String clientOne = "client1";
        final String clientTwo = "client2";
        final String originalVersion = "Do or do not, there is no try.";
        final String versionTwo = "Do or do not, there is no try!";
        final String versionThree = "Do or do nothing, there is no try!";

        final ClientSyncEngine<String, DiffMatchPatchEdit> clientOneSyncEngine = clientSyncEngine();
        final ClientSyncEngine<String, DiffMatchPatchEdit> clientTwoSyncEngine = clientSyncEngine();
        // inject the client document into the client engine.
        clientOneSyncEngine.addDocument(newClientDoc(documentId, originalVersion, clientOne));
        clientTwoSyncEngine.addDocument(newClientDoc(documentId, originalVersion, clientTwo));

        // add the server documents into the server engine for both clients.
        final DefaultDocument<String> serverDocument = newDoc(documentId, originalVersion);
        serverSyncEngine.addSubscriber(new MockSubscriber(clientOne), serverDocument);
        serverSyncEngine.addSubscriber(new MockSubscriber(clientTwo), serverDocument);

        // create an update originating from client1.
        serverSyncEngine.patch(clientOneSyncEngine.diff(newClientDoc(documentId, versionTwo, clientOne)));
        final PatchMessage<DiffMatchPatchEdit> clientOneServerPatchMessage = serverSyncEngine.diffs(documentId, clientOne);
        assertThat(clientOneServerPatchMessage.clientId(), equalTo(clientOne));
        assertThat(clientOneServerPatchMessage.documentId(), equalTo(documentId));
        assertThat(clientOneServerPatchMessage.edits().size(), is(1));
        final DiffMatchPatchEdit clientOneServerEdit = clientOneServerPatchMessage.edits().peek();
        assertThat(clientOneServerEdit.clientVersion(), is(1L));
        assertThat(clientOneServerEdit.serverVersion(), is(0L));
        assertThat(clientOneServerEdit.diffs().size(), is(1));
        assertThat(clientOneServerEdit.diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(clientOneServerEdit.diffs().get(0).text(), equalTo(versionTwo));
        // no patch required for clientOneSyncEngine as this was performed after the diff was taken.
        clientOneSyncEngine.patch(clientOneServerPatchMessage);

        final PatchMessage<DiffMatchPatchEdit> clientTwoServerPatchMessage = serverSyncEngine.diffs(documentId, clientTwo);
        assertThat(clientTwoServerPatchMessage.clientId(), equalTo(clientTwo));
        assertThat(clientTwoServerPatchMessage.documentId(), equalTo(documentId));
        assertThat(clientTwoServerPatchMessage.edits().size(), is(1));
        final DiffMatchPatchEdit clientTwoServerEdit = clientTwoServerPatchMessage.edits().peek();
        assertThat(clientTwoServerEdit.clientVersion(), is(0L));
        assertThat(clientTwoServerEdit.serverVersion(), is(0L));
        final LinkedList<DiffMatchPatchDiff> clientTwoServerDiffs = clientTwoServerEdit.diffs();
        assertThat(clientTwoServerDiffs.size(), is(3));
        assertThat(clientTwoServerDiffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(clientTwoServerDiffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(clientTwoServerDiffs.get(1).operation(), is(Operation.DELETE));
        assertThat(clientTwoServerDiffs.get(1).text(), equalTo("."));
        assertThat(clientTwoServerDiffs.get(2).operation(), is(Operation.ADD));
        assertThat(clientTwoServerDiffs.get(2).text(), equalTo("!"));
        clientTwoSyncEngine.patch(clientTwoServerPatchMessage);

        serverSyncEngine.patch(clientOneSyncEngine.diff(newClientDoc(documentId, versionThree, clientOne)));
        final DiffMatchPatchEdit thirdEdit = serverSyncEngine.diff(documentId, clientTwo);
        assertThat(thirdEdit.clientVersion(), is(0L));
        assertThat(thirdEdit.serverVersion(), is(1L));
        final LinkedList<DiffMatchPatchDiff> thirdDiffs = thirdEdit.diffs();
        assertThat(thirdDiffs.size(), is(3));
        assertThat(thirdDiffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(thirdDiffs.get(1).operation(), is(Operation.ADD));
        assertThat(thirdDiffs.get(1).text(), equalTo("hing"));
        assertThat(thirdDiffs.get(2).operation(), is(Operation.UNCHANGED));
    }

    private static ClientDocument<String> newClientDoc(final String documentId, final String content, final String clientId) {
        return new DefaultClientDocument<String>(documentId, clientId, content);
    }

    private static PatchMessage<DiffMatchPatchEdit> clientSideEdits(final String documentId,
                                         final String originalContent,
                                         final String clientId,
                                         final String updatedContent) {
        final ClientSyncEngine<String, DiffMatchPatchEdit> clientSyncEngine = clientSyncEngine();
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, clientId, originalContent));
        return clientSyncEngine.diff(new DefaultClientDocument<String>(documentId, clientId, updatedContent));
    }

    private static ClientSyncEngine<String, DiffMatchPatchEdit> clientSyncEngine() {
        final ClientDataStore<String, DiffMatchPatchEdit> clientDataStore = new ClientInMemoryDataStore<String, DiffMatchPatchEdit>();
        return new ClientSyncEngine<String, DiffMatchPatchEdit>(new DefaultClientSynchronizer(), clientDataStore);
    }

    private static DefaultDocument<String> newDoc(final String documentId, String content) {
        return new DefaultDocument<String>(documentId, content);
    }

    private static class MockSubscriber implements Subscriber<String> {

        private final String clientId;

        MockSubscriber(final String clientId) {
            this.clientId = clientId;
        }

        @Override
        public String clientId() {
            return clientId;
        }

        @Override
        public String channel() {
            return null;
        }

        @Override
        public void patched(PatchMessage<?> patchMessage) {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked")
            final Subscriber<String> subscriber = (Subscriber<String>) o;
            return clientId.equals(subscriber.clientId());
        }

        @Override
        public int hashCode() {
            return clientId.hashCode();
        }
    }

}
