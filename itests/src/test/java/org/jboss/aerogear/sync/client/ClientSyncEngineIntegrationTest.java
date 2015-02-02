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

import org.jboss.aerogear.sync.BackupShadowDocument;
import org.jboss.aerogear.sync.ClientDocument;
import org.jboss.aerogear.sync.DefaultClientDocument;
import org.jboss.aerogear.sync.DefaultDocument;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchEdit;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.ShadowDocument;
import org.jboss.aerogear.sync.diffmatchpatch.server.DiffMatchPatchServerSynchronizer;
import org.jboss.aerogear.sync.diffmatchpatch.client.DefaultClientSynchronizer;
import org.jboss.aerogear.sync.server.ServerInMemoryDataStore;
import org.jboss.aerogear.sync.server.ServerSyncEngine;
import org.jboss.aerogear.sync.server.Subscriber;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Queue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.*;
import static org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff.Operation;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientSyncEngineIntegrationTest {

    private ClientDataStore<String, DiffMatchPatchEdit> dataStore;
    private ClientSyncEngine<String, DiffMatchPatchEdit> clientSyncEngine;
    private ServerSyncEngine<String, DiffMatchPatchEdit> serverSyncEngine;

    @Before
    public void setup() {
        dataStore = new ClientInMemoryDataStore<String, DiffMatchPatchEdit>();
        clientSyncEngine = new ClientSyncEngine<String, DiffMatchPatchEdit>(new DefaultClientSynchronizer(), dataStore);
        serverSyncEngine = new ServerSyncEngine<String, DiffMatchPatchEdit>(new DiffMatchPatchServerSynchronizer(), new ServerInMemoryDataStore<String, DiffMatchPatchEdit>());
    }

    @Test
    public void addDocument() {
        final String docId = "123456";
        final String clientId = "client1";
        final String originalVersion = "Do or do not, there is no try.";

        clientSyncEngine.addDocument(newClientDoc(docId, originalVersion, clientId));

        final ClientDocument<String> clientDocument = dataStore.getClientDocument(docId, clientId);
        assertThat(clientDocument.clientId(), equalTo(clientId));
        assertThat(clientDocument.id(), equalTo(docId));
        assertThat(clientDocument.content(), equalTo(originalVersion));

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(docId, clientId);
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.document(), equalTo(clientDocument));

        final BackupShadowDocument<String> backupShadowDocument = dataStore.getBackupShadowDocument(docId, clientId);
        assertThat(backupShadowDocument.version(), is(0L));
        assertThat(backupShadowDocument.shadow(), equalTo(shadowDocument));
    }

    @Test
    public void diff() {
        final String docId = "123456";
        final String clientOne = "client1";
        final String originalVersion = "Do or do not, there is no try.";
        final String secondVersion = "Do or do not, there is no try!";

        clientSyncEngine.addDocument(newClientDoc(docId, originalVersion, clientOne));

        final PatchMessage<DiffMatchPatchEdit> patchMessage = clientSyncEngine.diff(newClientDoc(docId, secondVersion, clientOne));
        assertThat(patchMessage.documentId(), equalTo(docId));
        assertThat(patchMessage.clientId(), equalTo(clientOne));
        assertThat(patchMessage.edits().size(), is(1));
        final DiffMatchPatchEdit edit = patchMessage.edits().iterator().next();
        // client version is only incremented after the diff is taken. See shadowDocument asserts below.
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));

        final List<DiffMatchPatchDiff> diffs = edit.diff().diffs();
        assertThat(diffs.size(), is(3));
        assertThat(diffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(diffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(diffs.get(1).operation(), is(Operation.DELETE));
        assertThat(diffs.get(1).text(), equalTo("."));
        assertThat(diffs.get(2).operation(), is(Operation.ADD));
        assertThat(diffs.get(2).text(), equalTo("!"));

        final ClientDocument<String> document = dataStore.getShadowDocument(patchMessage.documentId(), patchMessage.clientId()).document();

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(patchMessage.documentId(), patchMessage.clientId());
        assertThat(shadowDocument.clientVersion(), is(1L));
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.document(), equalTo(document));
    }

    @Test
    public void patch() {
        final String docId = "123456";
        final Subscriber<?> subscriberOne = mock(Subscriber.class);
        when(subscriberOne.clientId()).thenReturn("client1");
        final Subscriber<?> subscriberTwo = mock(Subscriber.class);
        when(subscriberTwo.clientId()).thenReturn("client2");
        final String originalVersion = "Do or do not, there is no try.";
        final String secondVersion = "Do or do not, there is no try!";

        clientSyncEngine.addDocument(newClientDoc(docId, originalVersion, subscriberOne.clientId()));
        clientSyncEngine.addDocument(newClientDoc(docId, originalVersion, subscriberTwo.clientId()));
        serverSyncEngine.addSubscriber(subscriberOne, newDoc(docId, originalVersion));
        serverSyncEngine.addSubscriber(subscriberTwo, newDoc(docId, originalVersion));

        final PatchMessage<DiffMatchPatchEdit> patchMessage = clientSyncEngine.diff(newClientDoc(docId, secondVersion, subscriberOne.clientId()));
        assertThat(patchMessage.documentId(), equalTo(docId));
        assertThat(patchMessage.clientId(), equalTo(subscriberOne.clientId()));
        assertThat(patchMessage.edits().size(), is(1));
        final DiffMatchPatchEdit edit = patchMessage.edits().peek();
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        final List<DiffMatchPatchDiff> diffs = edit.diff().diffs();
        assertThat(diffs.size(), is(3));
        assertThat(diffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(diffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(diffs.get(1).operation(), is(Operation.DELETE));
        assertThat(diffs.get(1).text(), equalTo("."));
        assertThat(diffs.get(2).operation(), is(Operation.ADD));
        assertThat(diffs.get(2).text(), equalTo("!"));

        serverSyncEngine.patch(patchMessage);

        final PatchMessage<DiffMatchPatchEdit> serverPatchMessage = serverSyncEngine.diffs(docId, subscriberTwo.clientId());
        assertThat(serverPatchMessage.clientId(), equalTo(subscriberTwo.clientId()));
        assertThat(serverPatchMessage.documentId(), equalTo(docId));
        assertThat(serverPatchMessage.edits().size(), is(1));
        final DiffMatchPatchEdit serverEdit = serverPatchMessage.edits().peek();
        assertThat(serverEdit.clientVersion(), is(0L));
        assertThat(serverEdit.serverVersion(), is(0L));
        assertThat(serverEdit.diff().diffs().size(), is(3));
        final List<DiffMatchPatchDiff> serverDiffs = edit.diff().diffs();
        assertThat(serverDiffs.size(), is(3));
        assertThat(serverDiffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(serverDiffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(serverDiffs.get(1).operation(), is(Operation.DELETE));
        assertThat(serverDiffs.get(1).text(), equalTo("."));
        assertThat(serverDiffs.get(2).operation(), is(Operation.ADD));
        assertThat(serverDiffs.get(2).text(), equalTo("!"));

        clientSyncEngine.patch(serverPatchMessage);
        final Queue<DiffMatchPatchEdit> clientOneEdits = dataStore.getEdits(docId, subscriberTwo.clientId());
        assertThat(clientOneEdits.isEmpty(), is(true));
        final Queue<DiffMatchPatchEdit> clientTwoEdits = dataStore.getEdits(docId, subscriberTwo.clientId());
        assertThat(clientTwoEdits.isEmpty(), is(true));

        final DiffMatchPatchEdit serverEdit1 = serverSyncEngine.diff(docId, subscriberOne.clientId());
        assertThat(serverEdit1.diff().diffs().size(), is(1));
        assertThat(serverEdit1.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));

        final DiffMatchPatchEdit serverEdit2 = serverSyncEngine.diff(docId, subscriberTwo.clientId());
        assertThat(serverEdit2.diff().diffs().size(), is(1));
        assertThat(serverEdit2.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(serverEdit2.diff().diffs().get(0).text(), equalTo("Do or do not, there is no try!"));

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(docId, subscriberTwo.clientId());
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.serverVersion(), is(1L));
        assertThat(shadowDocument.document().content(), equalTo(secondVersion));

        final BackupShadowDocument<String> backupShadowDocument = dataStore.getBackupShadowDocument(docId, subscriberTwo.clientId());
        assertThat(backupShadowDocument.version(), is(0L));
        assertThat(backupShadowDocument.shadow().document().content(), equalTo(secondVersion));
    }

    private static ClientDocument<String> newClientDoc(final String documentId, final String content, final String clientId) {
        return new DefaultClientDocument<String>(documentId, clientId, content);
    }

    private static Document<String> newDoc(final String documentId, final String content) {
        return new DefaultDocument<String>(documentId, content);
    }
}
