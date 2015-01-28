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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import org.jboss.aerogear.sync.DefaultClientDocument;
import org.jboss.aerogear.sync.DefaultDocument;
import org.jboss.aerogear.sync.DefaultShadowDocument;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.ShadowDocument;
import org.jboss.aerogear.sync.server.ServerSynchronizer;

import java.util.Queue;

/**
 * A {@link ServerSynchronizer} implementation that can handle text documents.
 */
public class JsonPatchServerSynchronizer implements ServerSynchronizer<JsonNode, JsonPatchEdit> {

    @Override
    public JsonPatchEdit clientDiff(final Document<JsonNode> document, final ShadowDocument<JsonNode> shadowDocument) {
        final JsonNode shadowObject = shadowDocument.document().content();
        return JsonPatchEdit.withDocumentId(document.id())
                .clientId(shadowDocument.document().clientId())
                //.checksum(checksum(shadowObject))
                .diff(JsonDiff.asJsonPatch(document.content(), shadowObject))
                .build();
    }

    @Override
    public JsonPatchEdit serverDiff(final Document<JsonNode> document, final ShadowDocument<JsonNode> shadowDocument) {
        final JsonNode shadowObject = shadowDocument.document().content();
        return JsonPatchEdit.withDocumentId(document.id())
                .clientId(shadowDocument.document().clientId())
                .serverVersion(shadowDocument.serverVersion())
                .clientVersion(shadowDocument.clientVersion())
                //.checksum(checksum(shadowObject))
                .diff(JsonDiff.asJsonPatch(shadowObject, document.content()))
                .build();
    }

    @Override
    public ShadowDocument<JsonNode> patchShadow(final JsonPatchEdit edit, final ShadowDocument<JsonNode> shadowDocument) {
        final JsonNode content = patch(edit, shadowDocument.document().content());
        return new DefaultShadowDocument<JsonNode>(shadowDocument.serverVersion(), shadowDocument.clientVersion(),
                new DefaultClientDocument<JsonNode>(shadowDocument.document().id(), shadowDocument.document().clientId(), content));
    }

    private static JsonNode patch(final JsonPatchEdit edit, final JsonNode target) {
        JsonNode patched = target.deepCopy();
        try {
            for (JsonPatchDiff diff : edit.diffs()) {
                patched = diff.jsonPatch().apply(patched);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return patched;
    }

    @Override
    public Document<JsonNode> patchDocument(final JsonPatchEdit edit, final Document<JsonNode> document) {
        final JsonNode content = patch(edit, document.content());
        return new DefaultDocument<JsonNode>(document.id(), content);
    }

    @Override
    public PatchMessage<JsonPatchEdit> createPatchMessage(final String documentId,
                                                        final String clientId,
                                                        final Queue<JsonPatchEdit> edits) {
        return new JsonPatchMessage(documentId, clientId, edits);
    }

    @Override
    public PatchMessage<JsonPatchEdit> patchMessageFromJson(String json) {
        return JsonMapper.fromJson(json, JsonPatchMessage.class);
    }

}
