package org.jboss.aerogear.sync.diffmatchpatch.server;

import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchEdit;
import org.jboss.aerogear.sync.server.ServerInMemoryDataStore;
import org.junit.Test;

import java.util.Iterator;
import java.util.Queue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiffMatchPatchInMemoryDataStoreTest {

    @Test
    public void getEdits() {
        final String documentId = "12345";
        final String clientId = "client1";
        final ServerInMemoryDataStore<String, DiffMatchPatchEdit> dataStore = new ServerInMemoryDataStore<String, DiffMatchPatchEdit>();
        final DiffMatchPatchEdit editOne = DiffMatchPatchEdit.withChecksum("bogus").clientVersion(0).build();
        final DiffMatchPatchEdit editTwo = DiffMatchPatchEdit.withChecksum("bogus").clientVersion(1).build();
        dataStore.saveEdits(editOne, documentId, clientId);
        dataStore.saveEdits(editTwo, documentId, clientId);
        final Queue<DiffMatchPatchEdit> edits = dataStore.getEdits(documentId, clientId);
        assertThat(edits.size(), is(2));
        final Iterator<DiffMatchPatchEdit> iterator = edits.iterator();
        assertThat(iterator.next().clientVersion(), is(0L));
        assertThat(iterator.next().clientVersion(), is(1L));
    }
}
