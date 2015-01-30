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
package org.jboss.aerogear.sync.diffmatchpatch.client;

import static org.hamcrest.MatcherAssert.assertThat;

import org.jboss.aerogear.sync.*;
import org.jboss.aerogear.sync.client.ClientSynchronizer;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchEdit;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import static org.hamcrest.CoreMatchers.*;
import static org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff.Operation;

public class DefaultClientSynchronizerTest {

    private ClientSynchronizer<String, DiffMatchPatchEdit> clientSynchronizer;

    @Before
    public void createDocuments() {
        clientSynchronizer = new DefaultClientSynchronizer();
    }

    @Test
    public void diff() {
        final String documentId = "1234";
        final String clientId = "client1";
        final String original = "Do or do not, there is no try.";
        final String update = "Do or do not, there is no try!";
        final ShadowDocument<String> clientShadow = shadowDocument(documentId, clientId, original);

        final DiffMatchPatchEdit edit = clientSynchronizer.clientDiff(clientShadow, newDoc(documentId, clientId, update));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        assertThat(edit.clientId(), is(clientId));
        assertThat(edit.diffs().size(), is(3));
        final List<DiffMatchPatchDiff> diffs = edit.diffs();
        assertThat(diffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(diffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(diffs.get(1).operation(), is(Operation.DELETE));
        assertThat(diffs.get(1).text(), equalTo("!"));
        assertThat(diffs.get(2).operation(), is(Operation.ADD));
        assertThat(diffs.get(2).text(), equalTo("."));
    }

    @Test
    public void patchShadow() {
        final String documentId = "1234";
        final String clientId = "client1";
        final String originalVersion = "Do or do not, there is no try.";
        final String updatedVersion = "Do or do not, there is no try!";
        final ShadowDocument<String> clientShadow = shadowDocument(documentId, clientId, originalVersion);

        final DiffMatchPatchEdit edit = DiffMatchPatchEdit.withDocumentId(documentId)
                .clientId(clientId)
                .unchanged("Do or do not, there is no try")
                .delete(".")
                .add("!")
                .build();
        final ShadowDocument<String> patchedShadow = clientSynchronizer.patchShadow(edit, clientShadow);
        assertThat(patchedShadow.document().content(), equalTo(updatedVersion));
    }

    @Test
    public void patchDocument() {
        final String documentId = "1234";
        final String clientId = "client1";
        final String originalVersion = "Do or do not, there is no try.";
        final String updatedVersion = "Do or do nothing, there is no try.";
        final ClientDocument<String> clientShadow = new DefaultClientDocument<String>(documentId, clientId, originalVersion);

        final DiffMatchPatchEdit edit = DiffMatchPatchEdit.withDocumentId(documentId)
                .clientId(clientId)
                .unchanged("Do or do not")
                .add("hing")
                .unchanged(", there is no try.")
                .build();
        final ClientDocument<String> patchedDocument = clientSynchronizer.patchDocument(edit, clientShadow);
        assertThat(patchedDocument.content(), equalTo(updatedVersion));
    }

    private static ShadowDocument<String> shadowDocument(final String documentId,
                                                         final String clientId,
                                                         final String content) {
        final ClientDocument<String> clientDoc = new DefaultClientDocument<String>(documentId, clientId, content);
        return new DefaultShadowDocument<String>(0, 0, clientDoc);
    }

    private static ClientDocument<String> newDoc(final String documentId, final String clientId, final String content) {
        return new DefaultClientDocument<String>(documentId, clientId, content);
    }
}
