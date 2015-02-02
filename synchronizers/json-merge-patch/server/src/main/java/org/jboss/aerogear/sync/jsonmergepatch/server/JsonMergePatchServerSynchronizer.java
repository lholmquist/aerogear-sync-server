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
package org.jboss.aerogear.sync.jsonmergepatch.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.jboss.aerogear.sync.DefaultClientDocument;
import org.jboss.aerogear.sync.DefaultDocument;
import org.jboss.aerogear.sync.DefaultShadowDocument;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.ShadowDocument;
import org.jboss.aerogear.sync.jsonmergepatch.JsonMapper;
import org.jboss.aerogear.sync.jsonmergepatch.JsonMergePatchEdit;
import org.jboss.aerogear.sync.jsonmergepatch.JsonMergePatchMessage;
import org.jboss.aerogear.sync.server.ServerSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Queue;

/**
 * A {@link ServerSynchronizer} implementation that can handle text documents.
 */
public class JsonMergePatchServerSynchronizer implements ServerSynchronizer<JsonNode, JsonMergePatchEdit> {

    private static final String UTF_8 = Charset.forName("UTF-8").displayName();
    private static final Logger logger = LoggerFactory.getLogger(JsonMergePatchServerSynchronizer.class);

    @Override
    public JsonMergePatchEdit clientDiff(final Document<JsonNode> document, final ShadowDocument<JsonNode> shadowDocument) {
        final JsonNode shadowObject = shadowDocument.document().content();
        return JsonMergePatchEdit.withDocumentId(document.id())
                .clientId(shadowDocument.document().clientId())
                .checksum(checksum(shadowObject))
                .diff(patchFromJsonNode(shadowObject))
                .build();
    }

    @Override
    public JsonMergePatchEdit serverDiff(final Document<JsonNode> document, final ShadowDocument<JsonNode> shadowDocument) {
        final JsonNode shadowObject = shadowDocument.document().content();
        return JsonMergePatchEdit.withDocumentId(document.id())
                .clientId(shadowDocument.document().clientId())
                .serverVersion(shadowDocument.serverVersion())
                .clientVersion(shadowDocument.clientVersion())
                .checksum(checksum(shadowObject))
                .diff(patchFromJsonNode(document.content()))
                .build();
    }

    @Override
    public ShadowDocument<JsonNode> patchShadow(final JsonMergePatchEdit edit, final ShadowDocument<JsonNode> shadowDocument) {
        final JsonNode content = patch(edit, shadowDocument.document().content());
        return new DefaultShadowDocument<JsonNode>(shadowDocument.serverVersion(), shadowDocument.clientVersion(),
                new DefaultClientDocument<JsonNode>(shadowDocument.document().id(), shadowDocument.document().clientId(), content));
    }

    @Override
    public Document<JsonNode> patchDocument(final JsonMergePatchEdit edit, final Document<JsonNode> document) {
        final JsonNode content = patch(edit, document.content());
        return new DefaultDocument<JsonNode>(document.id(), content);
    }

    @Override
    public PatchMessage<JsonMergePatchEdit> createPatchMessage(final String documentId,
                                                        final String clientId,
                                                        final Queue<JsonMergePatchEdit> edits) {
        return new JsonMergePatchMessage(documentId, clientId, edits);
    }

    @Override
    public PatchMessage<JsonMergePatchEdit> patchMessageFromJson(String json) {
        return JsonMapper.fromJson(json, JsonMergePatchMessage.class);
    }

    @Override
    public Document<JsonNode> documentFromJson(JsonNode json) {
        return new DefaultDocument<JsonNode>(json.get("id").asText(), json.get("content"));
    }

    private static JsonNode patch(final JsonMergePatchEdit edit, final JsonNode target) {
        try {
            return edit.diff().jsonMergePatch().apply(target);
        } catch (final JsonPatchException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String checksum(final JsonNode content) {
        try {
            final MessageDigest md = MessageDigest.getInstance( "SHA1" );
            md.update(content.asText().getBytes(UTF_8));
            return new BigInteger(1, md.digest()).toString(16);
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    private static JsonMergePatch patchFromJsonNode(final JsonNode content) {
        try {
            return JsonMergePatch.fromJson(content);
        } catch (final JsonPatchException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
