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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.aerogear.sync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff.Operation;
import org.jboss.aerogear.sync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchEdit;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchMessage;
import org.jboss.aerogear.sync.diffmatchpatch.JsonMapper;
import org.jboss.aerogear.sync.diffmatchpatch.client.DefaultClientSynchronizer;
import org.jboss.aerogear.sync.diffmatchpatch.server.DiffMatchPatchServerSynchronizer;
import org.jboss.aerogear.sync.server.ServerInMemoryDataStore;
import org.jboss.aerogear.sync.server.ServerSyncEngine;
import org.jboss.aerogear.sync.server.ServerSynchronizer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jboss.aerogear.sync.diffmatchpatch.JsonMapper.fromJson;

public class DiffSyncHandlerTest {

    @Test
    public void unknownMessageType() {
        final EmbeddedChannel channel = embeddedChannel();
        final JsonNode json = writeTextFrame(message("bogus").toString(), channel);
        assertThat(json.get("result").asText(), equalTo("Unknown msgType 'bogus'"));
    }

    @Test
    public void addDocument() {
        final EmbeddedChannel channel = embeddedChannel();
        final String docId = UUID.randomUUID().toString();
        final String clientId = "client1";
        final PatchMessage<DiffMatchPatchEdit> patchMessage = sendAddDoc(docId, clientId, "Once upon a time", channel);
        assertThat(patchMessage.documentId(), equalTo(docId));
        assertThat(patchMessage.clientId(), equalTo(clientId));
        assertThat(patchMessage.edits().size(), is(1));
        assertThat(patchMessage.edits().peek().diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
    }

    @Test
    public void addDocumentObjectContent() {
        final EmbeddedChannel channel = embeddedChannel();
        final String docId = UUID.randomUUID().toString();
        final String clientId = "client1";
        final String content = "{\"content\": {\"name\": \"Dr.Rosen\"}}";
        final PatchMessage<DiffMatchPatchEdit> patchMessage = sendAddDoc(docId, clientId, content, channel);
        assertThat(patchMessage.documentId(), equalTo(docId));
        assertThat(patchMessage.clientId(), equalTo(clientId));
        assertThat(patchMessage.edits().size(), is(1));
        assertThat(patchMessage.edits().peek().diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
    }

    @Test
    public void addDocumentArrayContent() throws Exception {
        final EmbeddedChannel channel = embeddedChannel();
        final String docId = UUID.randomUUID().toString();
        final String clientId = "client1";
        final String content = "{\"content\": [\"one\", \"two\"]}";
        final PatchMessage<DiffMatchPatchEdit> patchMessage = sendAddDoc(docId, clientId, content, channel);
        assertThat(patchMessage.documentId(), equalTo(docId));
        assertThat(patchMessage.clientId(), equalTo(clientId));
        assertThat(patchMessage.edits().size(), is(1));
        assertThat(patchMessage.edits().peek().diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(patchMessage.edits().peek().diff().diffs().get(0).text(), equalTo(content));
    }

    @Test
    public void addDocumentAlreadyExisting() {
        final EmbeddedChannel channel = embeddedChannel();
        final String docId = UUID.randomUUID().toString();
        final String clientOneId = "client1";
        final String clientTwoId = "client2";
        final String content = "{\"content\": {\"name\": \"Dr.Rosen\"}}";

        final PatchMessage<DiffMatchPatchEdit> patchMessageOne = sendAddDoc(docId, clientOneId, content, channel);
        assertThat(patchMessageOne.documentId(), equalTo(docId));
        assertThat(patchMessageOne.clientId(), equalTo(clientOneId));
        assertThat(patchMessageOne.edits().size(), is(1));
        final DiffMatchPatchEdit editOne = patchMessageOne.edits().peek();
        assertThat(editOne.clientVersion(), is(0L));
        assertThat(editOne.serverVersion(), is(1L));
        assertThat(editOne.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));

        final PatchMessage<DiffMatchPatchEdit> patchMessageTwo = sendAddDoc(docId, clientTwoId, content, channel);
        assertThat(patchMessageTwo.documentId(), equalTo(docId));
        assertThat(patchMessageTwo.clientId(), equalTo(clientTwoId));
        assertThat(patchMessageTwo.edits().size(), is(1));
        final DiffMatchPatchEdit editTwo = patchMessageTwo.edits().peek();
        assertThat(editTwo.clientVersion(), is(-1L));
        assertThat(editTwo.serverVersion(), is(1L));
        assertThat(editTwo.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
    }

    @Test
    public void addDocumentWithoutContent() {
        final ServerInMemoryDataStore<String, DiffMatchPatchEdit> dataStore = new ServerInMemoryDataStore<String, DiffMatchPatchEdit>();
        final EmbeddedChannel channel1 = embeddedChannel(dataStore);
        final EmbeddedChannel channel2 = embeddedChannel(dataStore);
        final String docId = UUID.randomUUID().toString();
        final String client1Id = "client1";
        final String client2Id = "client2";
        final String baseContent = "You shall not pass";

        // client1 sends the initial base document.
        sendAddDoc(docId, client1Id, baseContent, channel1);

        // client2 sends a add document request without any content.
        final PatchMessage<DiffMatchPatchEdit> patchMessage = sendAddDoc(docId, client2Id, channel2);
        assertThat(patchMessage.documentId(), equalTo(docId));
        assertThat(patchMessage.clientId(), equalTo(client2Id));
        assertThat(patchMessage.edits().size(), is(1));
        final DiffMatchPatchEdit edit = patchMessage.edits().peek();
        assertThat(edit.clientVersion(), is(-1L));
        assertThat(edit.serverVersion(), is(1L));
        assertThat(edit.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(edit.diff().diffs().get(0).text(), equalTo(baseContent));
    }

    @Test
    public void addDocumentWithContentConcurrent() throws Exception {
        final ExecutorService executorService = Executors.newCachedThreadPool();
        final int iterations = 100;
        final CountDownLatch await = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(iterations);
        final String content = "You shall not pass!";
        final List<Future<PatchMessage<DiffMatchPatchEdit>>> futures = new ArrayList<Future<PatchMessage<DiffMatchPatchEdit>>>();
        final String client2Id = "client2";
        for (int i = 0 ; i < iterations; i++) {
            final String docId = UUID.randomUUID().toString();
            final ServerInMemoryDataStore<String, DiffMatchPatchEdit> dataStore = new ServerInMemoryDataStore<String, DiffMatchPatchEdit>();
            final EmbeddedChannel channel1 = embeddedChannel(dataStore);
            final EmbeddedChannel channel2 = embeddedChannel(dataStore);
            executorService.submit(new AddDocumentTask(channel1, docId, "client1", content, await, latch));
            final Future<PatchMessage<DiffMatchPatchEdit>> future = executorService.submit(new AddDocumentTask(channel2, docId, client2Id, content, await, latch));
            futures.add(future);
        }
        await.countDown();
        latch.await();
        for (Future<PatchMessage<DiffMatchPatchEdit>> future: futures) {
            final PatchMessage<DiffMatchPatchEdit> patchMessage = future.get();
            assertThat(patchMessage.clientId(), equalTo(client2Id));
            assertThat(patchMessage.edits().size(), is(1));
            final DiffMatchPatchEdit edit = patchMessage.edits().peek();
            assertThat(edit.serverVersion(), is(1L));
            assertThat(edit.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
            assertThat(edit.diff().diffs().get(0).text(), equalTo(content));
        }
        executorService.shutdown();
    }

    @Test
    public void addDocumentWithoutContentConcurrent() throws Exception {
        final ExecutorService executorService = Executors.newCachedThreadPool();
        final int iterations = 100;
        final CountDownLatch await = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(iterations);
        final String content = "You shall not pass!";
        final List<Future<PatchMessage<DiffMatchPatchEdit>>> futures = new ArrayList<Future<PatchMessage<DiffMatchPatchEdit>>>();
        final String client2Id = "client2";
        for (int i = 0 ; i < iterations; i++) {
            final String docId = UUID.randomUUID().toString();
            final ServerInMemoryDataStore<String, DiffMatchPatchEdit> dataStore = new ServerInMemoryDataStore<String, DiffMatchPatchEdit>();
            final EmbeddedChannel channel1 = embeddedChannel(dataStore);
            final EmbeddedChannel channel2 = embeddedChannel(dataStore);
            executorService.submit(new AddDocumentTask(channel1, docId, "client1", content, await, latch));
            final Future<PatchMessage<DiffMatchPatchEdit>> future = executorService.submit(new AddDocumentTask(channel2, docId, client2Id, null, await, latch));
            futures.add(future);
        }
        await.countDown();
        latch.await();
        for (Future<PatchMessage<DiffMatchPatchEdit>> future: futures) {
            final PatchMessage<DiffMatchPatchEdit> patchMessage = future.get();
            assertThat(patchMessage.clientId(), equalTo(client2Id));
            // patchMessage can be empty if there is not yet and underlying document in the data store.
            // in our case this means that the first thread has not yet been executed.
            if (!patchMessage.edits().isEmpty()) {
                assertThat(patchMessage.edits().size(), is(1));
                final DiffMatchPatchEdit edit = patchMessage.edits().peek();
                assertThat(edit.serverVersion(), is(1L));
                assertThat(edit.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
                assertThat(edit.diff().diffs().get(0).text(), equalTo(content));
            }
        }
        executorService.shutdown();
    }

    private static class AddDocumentTask implements Callable<PatchMessage<DiffMatchPatchEdit>> {

        private final EmbeddedChannel channel;
        protected final String docId;
        protected final String clientId;
        protected final String content;
        private final CountDownLatch await;
        private final CountDownLatch latch;

        AddDocumentTask(final EmbeddedChannel channel,
                        final String docId,
                        final String clientId,
                        final String content,
                        final CountDownLatch await,
                        final CountDownLatch latch) {
            this.channel = channel;
            this.docId = docId;
            this.clientId = clientId;
            this.content = content;
            this.latch = latch;
            this.await = await;
        }

        @Override
        public PatchMessage<DiffMatchPatchEdit> call() throws InterruptedException {
            try {
                await.await();
                return sendAddDoc(docId, clientId, content, channel);
            } finally {
                latch.countDown();
            }
        }
    }

    @Test
    public void patch() {
        final ServerInMemoryDataStore<String, DiffMatchPatchEdit> dataStore = new ServerInMemoryDataStore<String, DiffMatchPatchEdit>();
        final EmbeddedChannel channel1 = embeddedChannel(dataStore);
        final EmbeddedChannel channel2 = embeddedChannel(dataStore);
        final String docId = UUID.randomUUID().toString();
        final String originalContent = "{\"content\": \"Do or do not, there is no try.\"}";
        final String updatedContent = "{\"content\": \"Do or do not, there is no try!\"}";
        final String client1Id = "client1";
        final String client2Id = "client2";

        // add same document but with two different clients/channels.
        sendAddDocMsg(docId, client1Id, originalContent, channel1);
        sendAddDocMsg(docId, client2Id, originalContent, channel2);

        final PatchMessage<DiffMatchPatchEdit> clientEdit = generateClientSideEdits(docId, originalContent, client1Id, updatedContent);
        final PatchMessage<DiffMatchPatchEdit> patchMessage = sendEdit(clientEdit, channel1);
        assertThat(patchMessage.documentId(), equalTo(docId));
        assertThat(patchMessage.clientId(), equalTo(client1Id));
        assertThat(patchMessage.edits().size(), is(1));
        assertThat(patchMessage.edits().peek().diff().diffs().get(0).operation(), is(Operation.UNCHANGED));

        // client1 should not get an update as it was the one making the change.
        assertThat(channel1.readOutbound(), is(nullValue()));

        // get the update from channel2.
        final TextWebSocketFrame serverUpdate = channel2.readOutbound();
        final PatchMessage<DiffMatchPatchEdit> serverUpdates = fromJson(serverUpdate.text(), DiffMatchPatchMessage.class);
        assertThat(serverUpdates.documentId(), equalTo(docId));
        assertThat(serverUpdates.clientId(), equalTo(client2Id));
        final DiffMatchPatchEdit edit = serverUpdates.edits().peek();
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        assertThat(edit.diff().diffs().size(), is(4));
        assertThat(edit.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(edit.diff().diffs().get(1).operation(), is(Operation.DELETE));
        assertThat(edit.diff().diffs().get(1).text(), equalTo("."));
        assertThat(edit.diff().diffs().get(2).operation(), is(Operation.ADD));
        assertThat(edit.diff().diffs().get(2).text(), equalTo("!"));
        assertThat(edit.diff().diffs().get(3).operation(), is(Operation.UNCHANGED));
    }

    @Test
    public void patchJedi() {
        final ClientSyncEngine<String, DiffMatchPatchEdit> clientSyncEngine = newClientSyncEngine();
        final ServerInMemoryDataStore<String, DiffMatchPatchEdit> dataStore = new ServerInMemoryDataStore<String, DiffMatchPatchEdit>();
        final EmbeddedChannel channel1 = embeddedChannel(dataStore);
        final EmbeddedChannel channel2 = embeddedChannel(dataStore);
        final String docId = UUID.randomUUID().toString();
        final String client1Id = "client1";
        final String client2Id = "client2";
        final String original = "I'm a Jedi";
        final String updateOne = "I'm a Sith";
        final String updateTwo = "Oh Yeah";

        // Add original document using client1/channel1
        final PatchMessage<DiffMatchPatchEdit> addPatchClient1 = sendAddDoc(docId, client1Id, original, channel1);
        assertThat(addPatchClient1.documentId(), equalTo(docId));
        assertThat(addPatchClient1.clientId(), equalTo(client1Id));
        assertThat(addPatchClient1.edits().size(), is(1));
        final DiffMatchPatchEdit patchOne = addPatchClient1.edits().peek();
        assertThat(patchOne.clientVersion(), is(0L));
        assertThat(patchOne.serverVersion(), is(1L));
        assertThat(patchOne.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));

        // Add document using client2/channel2
        final PatchMessage<DiffMatchPatchEdit> addPatchClient2 = sendAddDoc(docId, client2Id, original, channel2);
        assertThat(addPatchClient2.documentId(), equalTo(docId));
        assertThat(addPatchClient2.clientId(), equalTo(client2Id));
        assertThat(addPatchClient2.edits().size(), is(1));
        final DiffMatchPatchEdit patchTwo = addPatchClient2.edits().peek();
        assertThat(patchTwo.clientVersion(), is(-1L));
        assertThat(patchTwo.serverVersion(), is(1L));
        assertThat(patchTwo.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));

        // Add the document to the client sync engine. Only used to help produce diffs.
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(docId, client1Id, original));
        final PatchMessage<DiffMatchPatchEdit> clientEdit = clientSyncEngine.diff(new DefaultClientDocument<String>(docId, client1Id, updateOne));

        final PatchMessage<DiffMatchPatchEdit> patchMessage = sendEdit(clientEdit, channel1);
        assertThat(patchMessage.documentId(), equalTo(docId));
        assertThat(patchMessage.clientId(), equalTo(client1Id));
        assertThat(patchMessage.edits().size(), is(1));
        assertThat(patchMessage.edits().peek().diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(patchMessage.edits().peek().clientVersion(), is(1L));
        assertThat(patchMessage.edits().peek().serverVersion(), is(0L));

        // patch the client engine so that version are updated and edits cleared
        clientSyncEngine.patch(patchMessage);

        // get the update from channel2.
        final TextWebSocketFrame serverUpdateOne = channel2.readOutbound();
        final PatchMessage<DiffMatchPatchEdit> serverUpdates = fromJson(serverUpdateOne.text(), DiffMatchPatchMessage.class);
        assertThat(serverUpdates.documentId(), equalTo(docId));
        assertThat(serverUpdates.clientId(), equalTo(client2Id));
        final DiffMatchPatchEdit editOne = serverUpdates.edits().peek();
        assertThat(editOne.clientVersion(), is(0L));
        assertThat(editOne.serverVersion(), is(0L));
        assertThat(editOne.diff().diffs().size(), is(5));
        assertThat(editOne.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(editOne.diff().diffs().get(0).text(), equalTo("I'm a "));
        assertThat(editOne.diff().diffs().get(1).operation(), is(Operation.DELETE));
        assertThat(editOne.diff().diffs().get(1).text(), equalTo("Jed"));
        assertThat(editOne.diff().diffs().get(2).operation(), is(Operation.ADD));
        assertThat(editOne.diff().diffs().get(2).text(), equalTo("S"));
        assertThat(editOne.diff().diffs().get(3).operation(), is(Operation.UNCHANGED));
        assertThat(editOne.diff().diffs().get(3).text(), equalTo("i"));
        assertThat(editOne.diff().diffs().get(4).operation(), is(Operation.ADD));
        assertThat(editOne.diff().diffs().get(4).text(), equalTo("th"));

        final PatchMessage<DiffMatchPatchEdit> clientEditTwo = clientSyncEngine.diff(new DefaultClientDocument<String>(docId, client1Id, updateTwo));
        final PatchMessage<DiffMatchPatchEdit> patchMessageTwo = sendEdit(clientEditTwo, channel1);
        assertThat(patchMessageTwo.edits().size(), is(1));
        assertThat(patchMessageTwo.edits().peek().diff().diffs().get(0).operation(), is(Operation.UNCHANGED));

        final TextWebSocketFrame serverUpdateTwo = channel2.readOutbound();
        final PatchMessage<DiffMatchPatchEdit> serverUpdatesTwo = fromJson(serverUpdateTwo.text(), DiffMatchPatchMessage.class);
        assertThat(serverUpdatesTwo.documentId(), equalTo(docId));
        assertThat(serverUpdatesTwo.clientId(), equalTo(client2Id));
        assertThat(serverUpdatesTwo.edits().size(), is(2));
        // just remove the first edit. We have already verified it but since we have not
        // sent an acknowledgement to the server, the server thinks that we never got it.
        serverUpdatesTwo.edits().remove();

        final DiffMatchPatchEdit editTwo = serverUpdatesTwo.edits().remove();
        assertThat(editTwo.clientVersion(), is(0L));
        assertThat(editTwo.serverVersion(), is(1L));

        assertThat(editTwo.diff().diffs().size(), is(7));
        assertThat(editTwo.diff().diffs().get(0).operation(), is(Operation.DELETE));
        assertThat(editTwo.diff().diffs().get(0).text(), equalTo("I'm"));
        assertThat(editTwo.diff().diffs().get(1).operation(), is(Operation.ADD));
        assertThat(editTwo.diff().diffs().get(1).text(), equalTo("Oh"));
        assertThat(editTwo.diff().diffs().get(2).operation(), is(Operation.UNCHANGED));
        assertThat(editTwo.diff().diffs().get(2).text(), equalTo(" "));
        assertThat(editTwo.diff().diffs().get(3).operation(), is(Operation.ADD));
        assertThat(editTwo.diff().diffs().get(3).text(), equalTo("Ye"));
        assertThat(editTwo.diff().diffs().get(4).operation(), is(Operation.UNCHANGED));
        assertThat(editTwo.diff().diffs().get(4).text(), equalTo("a"));
        assertThat(editTwo.diff().diffs().get(5).operation(), is(Operation.DELETE));
        assertThat(editTwo.diff().diffs().get(5).text(), equalTo(" Sit"));
        assertThat(editTwo.diff().diffs().get(6).operation(), is(Operation.UNCHANGED));
        assertThat(editTwo.diff().diffs().get(6).text(), equalTo("h"));
    }

    @Test
    public void patchCompletReplacementOfContent() {
        final ClientSyncEngine<String, DiffMatchPatchEdit> clientSyncEngine = newClientSyncEngine();
        final ServerInMemoryDataStore<String, DiffMatchPatchEdit> dataStore = new ServerInMemoryDataStore<String, DiffMatchPatchEdit>();
        final EmbeddedChannel channel1 = embeddedChannel(dataStore);
        final EmbeddedChannel channel2 = embeddedChannel(dataStore);
        final String docId = UUID.randomUUID().toString();
        final String client1Id = "client1";
        final String client2Id = "client2";
        final String original = "Beve";
        final String updateOne = "I'm the man";

        // Add original document using client1/channal1
        final PatchMessage<DiffMatchPatchEdit> addPatchClient1 = sendAddDoc(docId, client1Id, original, channel1);
        assertThat(addPatchClient1.documentId(), equalTo(docId));
        assertThat(addPatchClient1.clientId(), equalTo(client1Id));
        assertThat(addPatchClient1.edits().size(), is(1));
        final DiffMatchPatchEdit patchOne = addPatchClient1.edits().peek();
        assertThat(patchOne.clientVersion(), is(0L));
        assertThat(patchOne.serverVersion(), is(1L));
        assertThat(patchOne.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));

        // Add document using client2/channel2
        final PatchMessage<DiffMatchPatchEdit> addPatchClient2 = sendAddDoc(docId, client2Id, original, channel2);
        assertThat(addPatchClient2.documentId(), equalTo(docId));
        assertThat(addPatchClient2.clientId(), equalTo(client2Id));
        assertThat(addPatchClient2.edits().size(), is(1));
        final DiffMatchPatchEdit patchTwo = addPatchClient2.edits().peek();
        assertThat(patchTwo.clientVersion(), is(-1L));
        assertThat(patchTwo.serverVersion(), is(1L));
        assertThat(patchTwo.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));

        // Add the document to the client sync engine. Only used to help produce diffs.
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(docId, client1Id, original));
        final PatchMessage<DiffMatchPatchEdit> clientEdit = clientSyncEngine.diff(new DefaultClientDocument<String>(docId, client1Id, updateOne));

        final PatchMessage<DiffMatchPatchEdit> patchMessage = sendEdit(clientEdit, channel1);
        assertThat(patchMessage.documentId(), equalTo(docId));
        assertThat(patchMessage.clientId(), equalTo(client1Id));
        assertThat(patchMessage.edits().size(), is(1));
        assertThat(patchMessage.edits().peek().diff().diffs().get(0).operation(), is(Operation.UNCHANGED));

        // patch the client engine so that version are updated and edits cleared
        clientSyncEngine.patch(patchMessage);

        // get the update from channel2.
        final TextWebSocketFrame serverUpdateOne = channel2.readOutbound();
        final PatchMessage<DiffMatchPatchEdit> serverUpdates = fromJson(serverUpdateOne.text(), DiffMatchPatchMessage.class);
        assertThat(serverUpdates.documentId(), equalTo(docId));
        assertThat(serverUpdates.clientId(), equalTo(client2Id));
        final DiffMatchPatchEdit editOne = serverUpdates.edits().peek();
        assertThat(editOne.clientVersion(), is(0L));
        assertThat(editOne.serverVersion(), is(0L));

        assertThat(editOne.diff().diffs().size(), is(5));
        assertThat(editOne.diff().diffs().get(0).operation(), is(Operation.DELETE));
        assertThat(editOne.diff().diffs().get(0).text(), equalTo("B"));
        assertThat(editOne.diff().diffs().get(1).operation(), is(Operation.ADD));
        assertThat(editOne.diff().diffs().get(1).text(), equalTo("I'm th"));
        assertThat(editOne.diff().diffs().get(2).operation(), is(Operation.UNCHANGED));
        assertThat(editOne.diff().diffs().get(2).text(), equalTo("e"));
        assertThat(editOne.diff().diffs().get(3).operation(), is(Operation.DELETE));
        assertThat(editOne.diff().diffs().get(3).text(), equalTo("ve"));
        assertThat(editOne.diff().diffs().get(4).operation(), is(Operation.ADD));
        assertThat(editOne.diff().diffs().get(4).text(), equalTo(" man"));
    }

    private static PatchMessage<DiffMatchPatchEdit> sendEdit(final PatchMessage<DiffMatchPatchEdit> patchMessage, final EmbeddedChannel ch) {
        return fromJson(writeFrame(JsonMapper.toJson(patchMessage), ch), DiffMatchPatchMessage.class);
    }

    private static JsonNode sendAddDocMsg(final String docId,
                                          final String clientId,
                                          final String content,
                                          final EmbeddedChannel ch) {
        final ObjectNode docMsg = message("add");
        docMsg.put("msgType", "add");
        docMsg.put("id", docId);
        docMsg.put("clientId", clientId);
        docMsg.put("content", content);
        return writeTextFrame(docMsg.toString(), ch);
    }

    private static PatchMessage<DiffMatchPatchEdit> sendAddDoc(final String docId,
                                           final String clientId,
                                           final String content,
                                           final EmbeddedChannel ch) {
        final ObjectNode docMsg = message("add");
        docMsg.put("msgType", "add");
        docMsg.put("id", docId);
        docMsg.put("clientId", clientId);
        docMsg.put("content", content);
        return fromJson(writeFrame(docMsg.toString(), ch), DiffMatchPatchMessage.class);
    }

    private static PatchMessage<DiffMatchPatchEdit> sendAddDoc(final String docId, final String clientId, final EmbeddedChannel ch) {
        final ObjectNode docMsg = message("add");
        docMsg.put("msgType", "add");
        docMsg.put("id", docId);
        docMsg.put("clientId", clientId);
        return fromJson(writeFrame(docMsg.toString(), ch), DiffMatchPatchMessage.class);
    }

    private static ObjectNode message(final String type) {
        final ObjectNode jsonNode = JsonMapper.newObjectNode();
        jsonNode.put("msgType", type);
        return jsonNode;
    }

    private static JsonNode writeTextFrame(final String content, final EmbeddedChannel ch) {
        ch.writeInbound(textFrame(content));
        final TextWebSocketFrame textFrame = ch.readOutbound();
        return JsonMapper.asJsonNode(textFrame.text());
    }

    private static String writeFrame(final String content, final EmbeddedChannel ch) {
        ch.writeInbound(textFrame(content));
        final TextWebSocketFrame textFrame = ch.readOutbound();
        return textFrame.text();
    }

    private static TextWebSocketFrame textFrame(final String content) {
        return new TextWebSocketFrame(content);
    }

    private static EmbeddedChannel embeddedChannel() {
        return embeddedChannel(new ServerInMemoryDataStore<String, DiffMatchPatchEdit>());
    }

    private static EmbeddedChannel embeddedChannel(final ServerInMemoryDataStore<String, DiffMatchPatchEdit> dataStore) {
        final ServerSynchronizer<String, DiffMatchPatchEdit> synchronizer = new DiffMatchPatchServerSynchronizer();
        final ServerSyncEngine<String, DiffMatchPatchEdit> syncEngine = new ServerSyncEngine<String, DiffMatchPatchEdit>(synchronizer, dataStore);
        return new EmbeddedChannel(new DiffSyncHandler<String, DiffMatchPatchEdit>(syncEngine));
    }

    private static PatchMessage<DiffMatchPatchEdit> generateClientSideEdits(final String documentId,
                                                 final String originalContent,
                                                 final String clientId,
                                                 final String updatedContent) {
        final ClientSyncEngine<String, DiffMatchPatchEdit> clientSyncEngine = newClientSyncEngine();
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, clientId, originalContent));
        final DefaultClientDocument<String> doc = new DefaultClientDocument<String>(documentId, clientId, updatedContent);
        return clientSyncEngine.diff(doc);
    }

    private static ClientSyncEngine<String, DiffMatchPatchEdit> newClientSyncEngine() {
        return new ClientSyncEngine<String, DiffMatchPatchEdit>(new DefaultClientSynchronizer(),
                new ClientInMemoryDataStore<String, DiffMatchPatchEdit>());
    }

}
