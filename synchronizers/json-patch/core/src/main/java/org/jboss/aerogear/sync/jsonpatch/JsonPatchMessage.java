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
package org.jboss.aerogear.sync.jsonpatch;

import org.jboss.aerogear.sync.PatchMessage;

import java.util.Queue;

public class JsonPatchMessage implements PatchMessage<JsonPatchEdit> {

    private final String documentId;
    private final String clientId;
    private final Queue<JsonPatchEdit> edits;

    public JsonPatchMessage(final String documentId, final String clientId, final Queue<JsonPatchEdit> edits) {
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
    public Queue<JsonPatchEdit> edits() {
        return edits;
    }

    @Override
    public String toString() {
        return "JsonPatchMessage[documentId=" + documentId + ", clientId=" + clientId + ", edits=" + edits + ']';
    }

    @Override
    public String asJson() {
        return JsonMapper.toJson(this);
    }

    @Override
    public JsonPatchMessage fromJson(final String json) {
        return JsonMapper.fromJson(json, JsonPatchMessage.class);
    }
}
