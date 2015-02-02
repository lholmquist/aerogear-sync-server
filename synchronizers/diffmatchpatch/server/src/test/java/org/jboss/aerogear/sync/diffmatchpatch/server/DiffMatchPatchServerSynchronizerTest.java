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
package org.jboss.aerogear.sync.diffmatchpatch.server;

import org.jboss.aerogear.sync.DefaultClientDocument;
import org.jboss.aerogear.sync.DefaultDocument;
import org.jboss.aerogear.sync.DefaultShadowDocument;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff.Operation;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.ShadowDocument;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchEdit;
import org.jboss.aerogear.sync.server.ServerSynchronizer;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiffMatchPatchServerSynchronizerTest {

    @Test
    public void clientDiff() throws Exception {
        final ServerSynchronizer<String, DiffMatchPatchEdit> synchronizer = new DiffMatchPatchServerSynchronizer();
        final Document<String> document = new DefaultDocument<String>("1234", "test");
        final ShadowDocument<String> shadowDocument = shadowDocument("1234", "client1", "testing");

        final DiffMatchPatchEdit edit = synchronizer.clientDiff(document, shadowDocument);
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        assertThat(edit.diff().diffs().size(), is(2));
        assertThat(edit.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(edit.diff().diffs().get(0).text(), is("test"));
        assertThat(edit.diff().diffs().get(1).operation(), is(Operation.ADD));
        assertThat(edit.diff().diffs().get(1).text(), is("ing"));
    }

    @Test
    public void serverDiff() throws Exception {
        final ServerSynchronizer<String, DiffMatchPatchEdit> synchronizer = new DiffMatchPatchServerSynchronizer();
        final Document<String> document = new DefaultDocument<String>("1234", "test");
        final ShadowDocument<String> shadowDocument = shadowDocument("1234", "client1", "testing");

        final DiffMatchPatchEdit edit = synchronizer.serverDiff(document, shadowDocument);
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        assertThat(edit.diff().diffs().size(), is(2));
        assertThat(edit.diff().diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(edit.diff().diffs().get(0).text(), is("test"));
        assertThat(edit.diff().diffs().get(1).operation(), is(Operation.DELETE));
        assertThat(edit.diff().diffs().get(1).text(), is("ing"));
    }

    @Test
    public void patchShadow() throws Exception {
        final ServerSynchronizer<String, DiffMatchPatchEdit> synchronizer = new DiffMatchPatchServerSynchronizer();
        final Document<String> document = new DefaultDocument<String>("1234", "test");
        final ShadowDocument<String> shadowDocument = shadowDocument("1234", "client1", "testing");

        final DiffMatchPatchEdit edit = synchronizer.serverDiff(document, shadowDocument);
        final ShadowDocument<String> patchedShadow = synchronizer.patchShadow(edit, shadowDocument);
        assertThat(patchedShadow.document().content(), equalTo("test"));
    }

    @Test
    public void patchShadowFromClientDiff() throws Exception {
        final ServerSynchronizer<String, DiffMatchPatchEdit> synchronizer = new DiffMatchPatchServerSynchronizer();
        final Document<String> document = new DefaultDocument<String>("1234", "Beve");
        final ShadowDocument<String> shadowDocument = shadowDocument("1234", "client1", "I'm the man");

        final DiffMatchPatchEdit edit = synchronizer.clientDiff(document, shadowDocument);
        final ShadowDocument<String> patchedShadow = synchronizer.patchShadow(edit, shadowDocument);
        assertThat(patchedShadow.document().content(), equalTo("I'm the man"));
    }

    @Test
    public void patchShadowFromClientDiffCustomEdit() throws Exception {
        final ServerSynchronizer<String, DiffMatchPatchEdit> synchronizer = new DiffMatchPatchServerSynchronizer();
        final ShadowDocument<String> shadowDocument = shadowDocument("1234", "client1", "Beve");

        final DiffMatchPatchEdit edit1 = DiffMatchPatchEdit.withChecksum("bogus")
                .delete("B")
                .add("I'm th")
                .unchanged("e")
                .delete("ve")
                .add(" man")
                .build();
        final ShadowDocument<String> patchedShadow = synchronizer.patchShadow(edit1, shadowDocument);
        assertThat(patchedShadow.document().content(), equalTo("I'm the man"));
    }

    @Test
    public void patchDocument() throws Exception {
        final ServerSynchronizer<String, DiffMatchPatchEdit> synchronizer = new DiffMatchPatchServerSynchronizer();
        final Document<String> document = new DefaultDocument<String>("1234", "test");
        final ShadowDocument<String> shadowDocument = shadowDocument("1234", "client1", "testing");

        final DiffMatchPatchEdit edit = synchronizer.clientDiff(document, shadowDocument);
        final Document<String> patchedDocument = synchronizer.patchDocument(edit, document);
        assertThat(patchedDocument.content(), equalTo("testing"));
    }

    private static ShadowDocument<String> shadowDocument(final String documentId,
                                                         final String clientVersion,
                                                         final String content) {
        return new DefaultShadowDocument<String>(0L,
                0L,
                new DefaultClientDocument<String>(documentId, clientVersion, content));

    }
}
