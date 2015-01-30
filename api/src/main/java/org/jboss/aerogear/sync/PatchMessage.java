package org.jboss.aerogear.sync;

import java.util.Queue;

/**
 * Represents a stack of edits.
 */
public interface PatchMessage<T extends Edit> extends Payload<PatchMessage<T>> {

    /**
     * Identifies the client that this edit instance belongs to.
     *
     * @return {@code String} the client identifier.
     */
    String clientId();

    /**
     * Identifies the document that this edit is related to
     *
     * @return {@code String} the document documentId.
     */
    String documentId();

    /**
     * The individual {@link Edit}s.
     *
     * @return {@code Queue<Edit>} the individual edits.
     */
    Queue<T> edits();

}
