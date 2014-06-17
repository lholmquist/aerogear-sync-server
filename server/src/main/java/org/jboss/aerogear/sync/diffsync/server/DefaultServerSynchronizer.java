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
package org.jboss.aerogear.sync.diffsync.server;

import org.jboss.aerogear.sync.common.DiffMatchPatch;
import org.jboss.aerogear.sync.diffsync.ClientDocument;
import org.jboss.aerogear.sync.diffsync.DefaultClientDocument;
import org.jboss.aerogear.sync.diffsync.DefaultDiff;
import org.jboss.aerogear.sync.diffsync.DefaultDocument;
import org.jboss.aerogear.sync.diffsync.DefaultEdits;
import org.jboss.aerogear.sync.diffsync.DefaultShadowDocument;
import org.jboss.aerogear.sync.diffsync.Diff;
import org.jboss.aerogear.sync.diffsync.Document;
import org.jboss.aerogear.sync.diffsync.Edits;
import org.jboss.aerogear.sync.diffsync.ShadowDocument;

import java.util.LinkedList;

import static org.jboss.aerogear.sync.common.DiffMatchPatch.Operation;
import static org.jboss.aerogear.sync.common.DiffMatchPatch.Patch;
import static org.jboss.aerogear.sync.common.DiffMatchPatch.builder;
import static org.jboss.aerogear.sync.common.DiffMatchPatch.checksum;

/**
 * A {@link ServerSynchronizer} implementation that can handle text documents.
 */
public class DefaultServerSynchronizer implements ServerSynchronizer<String> {

    private final DiffMatchPatch diffMatchPatch;

    public DefaultServerSynchronizer() {
        this(builder().build());
    }

    public DefaultServerSynchronizer(final DiffMatchPatch diffMatchPatch) {
        this.diffMatchPatch = diffMatchPatch;
    }

    @Override
    public Edits diff(final Document<String> document, final ShadowDocument<String> shadowDocument) {
        final String shadowText = shadowDocument.document().content();
        final LinkedList<DiffMatchPatch.Diff> diffs = diffMatchPatch.diffMain(shadowText, document.content());
        return new DefaultEdits(shadowDocument.document().clientId(), document.id(), shadowDocument.clientVersion(), checksum(shadowText), asAeroGearDiffs(diffs));
    }

    @Override
    public ShadowDocument<String> patchShadow(final Edits edits, final ShadowDocument<String> shadowDocument) {
        final LinkedList<Patch> patches = patchesFrom(edits);
        final ClientDocument<String> doc = shadowDocument.document();
        final Object[] results = diffMatchPatch.patchApply(patches, doc.content());
        final boolean[] patchResults = (boolean[]) results[1];
        final ClientDocument<String> patchedDocument = new DefaultClientDocument<String>(doc.id(), (String) results[0], doc.clientId() );
        //TODO: results also contains a boolean array. Not sure what we should do with it.
        return new DefaultShadowDocument<String>(shadowDocument.serverVersion(), edits.version(), patchedDocument);
    }

    @Override
    public Document<String> patchDocument(final Edits edits, final Document<String> document) {
        final LinkedList<Patch> patches = patchesFrom(edits);
        final Object[] results = diffMatchPatch.patchApply(patches, document.content());
        //TODO: results also contains a boolean array. Not sure what we should do with it.
        return new DefaultDocument<String>(document.id(), (String) results[0]);
    }

    private LinkedList<Patch> patchesFrom(final Edits edits) {
        return diffMatchPatch.patchMake(asDiffUtilDiffs(edits.diffs()));
    }

    private static LinkedList<DiffMatchPatch.Diff> asDiffUtilDiffs(final LinkedList<Diff> diffs) {
        final LinkedList<DiffMatchPatch.Diff> dsf = new LinkedList<DiffMatchPatch.Diff>();
        for (Diff d : diffs) {
            dsf.add(new DiffMatchPatch.Diff(diffutilOp(d.operation()), d.text()));
        }
        return dsf;
    }

    private static LinkedList<Diff> asAeroGearDiffs(final LinkedList<DiffMatchPatch.Diff> diffs) {
        final LinkedList<Diff> syncDiffs = new LinkedList<Diff>();
        for (DiffMatchPatch.Diff diff : diffs) {
            syncDiffs.add(new DefaultDiff(aerogearOp(diff.operation), diff.text));
        }
        return syncDiffs;
    }

    private static Operation diffutilOp(final Diff.Operation operation) {
        switch (operation) {
            case DELETE:
                return Operation.DELETE;
            case ADD:
                return Operation.INSERT;
            case UNCHANGED:
                return Operation.EQUAL;
            default:
                throw new RuntimeException("Unsupported Operation: " + operation);
        }
    }

    private static Diff.Operation aerogearOp(final Operation operation) {
        switch (operation) {
            case DELETE:
                return Diff.Operation.DELETE;
            case INSERT:
                return Diff.Operation.ADD;
            case EQUAL:
                return Diff.Operation.UNCHANGED;
            default:
                throw new RuntimeException("Unsupported Operation: " + operation);
        }
    }

}