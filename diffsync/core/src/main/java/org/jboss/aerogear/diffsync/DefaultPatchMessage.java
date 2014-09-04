package org.jboss.aerogear.diffsync;

import java.util.Queue;

public class DefaultPatchMessage implements PatchMessage {

    private final String documentId;
    private final String clientId;
    private final Queue<Edit> edits;

    public DefaultPatchMessage(final String documentId, final String clientId, final Queue<Edit> edits) {
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
    public Queue<Edit> edits() {
        return edits;
    }

    @Override
    public String toString() {
        return "DefaultEdits[documentId=" + documentId + ", clientId=" + clientId + ", edits=" + edits + ']';
    }
}
