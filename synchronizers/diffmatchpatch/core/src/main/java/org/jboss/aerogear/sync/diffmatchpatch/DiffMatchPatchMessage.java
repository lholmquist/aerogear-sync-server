package org.jboss.aerogear.sync.diffmatchpatch;

import org.jboss.aerogear.sync.PatchMessage;

import java.util.Queue;

public class DiffMatchPatchMessage implements PatchMessage<DiffMatchPatchEdit> {

    private final String documentId;
    private final String clientId;
    private final Queue<DiffMatchPatchEdit> edits;

    public DiffMatchPatchMessage(final String documentId, final String clientId, final Queue<DiffMatchPatchEdit> edits) {
        this.documentId = documentId;
        this.clientId = clientId;
        this.edits = edits;
    }

    @Override
    public String documentId() {
        return documentId;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public Queue<DiffMatchPatchEdit> edits() {
        return edits;
    }

    @Override
    public String toString() {
        return "DefaultEdits[documentId=" + documentId + ", clientId=" + clientId + ", edits=" + edits + ']';
    }

    @Override
    public String asJson() {
        return JsonMapper.toJson(this);
    }

    @Override
    public DiffMatchPatchMessage fromJson(String json) {
        return JsonMapper.fromJson(json, DiffMatchPatchMessage.class);
    }
}
