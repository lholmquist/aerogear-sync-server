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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import org.jboss.aerogear.sync.jsonpatch.JsonPatchEdit.Builder;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public final class JsonMapper {

    private static ObjectMapper om = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        om = new ObjectMapper();
        final SimpleModule module = new SimpleModule("JsonPatch", new Version(1, 0, 0, null, "aerogear", "sync"));
        module.addDeserializer(JsonPatchEdit.class, new EditDeserializer());
        module.addSerializer(JsonPatchEdit.class, new EditSerializer());
        module.addDeserializer(JsonPatchMessage.class, new PatchMessageDeserializer());
        module.addSerializer(JsonPatchMessage.class, new PatchMessageSerializer());
        om.registerModule(module);
        return om;
    }

    private JsonMapper() {
    }

    /**
     * Transforms from JSON to the type specified.
     *
     * @param json the json to be transformed.
     * @param type the Java type that the JSON should be transformed to.
     * @param <T> the type the class to convert to
     * @return T an instance of the type populated with data from the json message.
     */
    public static <T> T fromJson(final String json, final Class<T> type) {
        try {
            return om.readValue(json, type);
        } catch (final Exception e) {
            throw new RuntimeException("error trying to parse json [" + json + ']', e);
        }
    }

    /**
     * Transforms from Java object notation to JSON.
     *
     * @param obj the Java object to transform into JSON.
     * @return {@code String} the json representation for the object.
     */
    public static String toJson(final Object obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (final Exception e) {
            throw new RuntimeException("error trying to parse json [" + obj + ']', e);
        }
    }

    public static String toString(final JsonNode jsonNode) {
        try {
            return om.writeValueAsString(jsonNode);
        } catch (final Exception e) {
            throw new RuntimeException("error trying to serialize jsonNode [" + jsonNode + ']', e);
        }
    }

    /**
     * Return a {@link JsonNode} for the passed in JSON string.
     *
     * @param json the string to be parsed.
     * @return JsonNode the JsonNode representing the passed-in JSON string.
     */
    public static JsonNode asJsonNode(final String json) {
        try {
            return om.readTree(json);
        } catch (final IOException e) {
            throw new RuntimeException("error trying to parse json [" + json + ']', e);
        }
    }

    public static ObjectNode newObjectNode() {
        return om.createObjectNode();
    }

    public static ArrayNode newArrayNode() {
        return om.createArrayNode();
    }

    private static class PatchMessageDeserializer extends JsonDeserializer<JsonPatchMessage> {

        @Override
        public JsonPatchMessage deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
            final ObjectCodec oc = jp.getCodec();
            final JsonNode node = oc.readTree(jp);
            final String documentId = node.get("id").asText();
            final String clientId = node.get("clientId").asText();
            final JsonNode jsonEdits = node.get("edits");
            final Queue<JsonPatchEdit> edits = new ConcurrentLinkedQueue<JsonPatchEdit>();
            if (jsonEdits.isArray()) {
                for (JsonNode edit : jsonEdits) {
                    if (edit.isNull()) {
                        continue;
                    }
                    final Builder eb = JsonPatchEdit.withDocumentId(documentId).clientId(clientId);
                    eb.clientVersion(edit.get("clientVersion").asLong());
                    eb.serverVersion(edit.get("serverVersion").asLong());
                    eb.checksum(edit.get("checksum").asText());
                    final JsonNode diffsNode = edit.get("diffs");
                    if (diffsNode.isArray()) {
                        for (JsonNode d : diffsNode) {
                            if (d.isNull()) {
                                continue;
                            }
                            final JsonPatch patch = JsonPatch.fromJson(d);
                            eb.diff(patch);
                        }
                    }
                    edits.add(eb.build());
                }
            }
            return new JsonPatchMessage(documentId, clientId, edits);
        }
    }

    private static class PatchMessageSerializer extends JsonSerializer<JsonPatchMessage> {

        @Override
        public void serialize(final JsonPatchMessage patchMessage,
                              final JsonGenerator jgen,
                              final SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("msgType", "patch");
            jgen.writeStringField("id", patchMessage.documentId());
            jgen.writeStringField("clientId", patchMessage.clientId());
            jgen.writeArrayFieldStart("edits");
            for (JsonPatchEdit edit : patchMessage.edits()) {
                if (edit == null) {
                    continue;
                }
                jgen.writeStartObject();
                jgen.writeStringField("clientId", edit.clientId());
                jgen.writeStringField("id", edit.documentId());
                jgen.writeNumberField("clientVersion", edit.clientVersion());
                jgen.writeNumberField("serverVersion", edit.serverVersion());
                jgen.writeStringField("checksum", edit.checksum());
                jgen.writeArrayFieldStart("diffs");
                if (!edit.diffs().isEmpty()) {
                    for (JsonPatchDiff diff : edit.diffs()) {
                        diff.jsonPatch().serialize(jgen, provider);
                    }
                    jgen.writeEndArray();
                }
                jgen.writeEndObject();
            }
            jgen.writeEndArray();
            jgen.writeEndObject();
        }
    }

    private static class EditDeserializer extends JsonDeserializer<JsonPatchEdit> {

        @Override
        public JsonPatchEdit deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
            final ObjectCodec oc = jp.getCodec();
            final JsonNode edit = oc.readTree(jp);
            final Builder eb = JsonPatchEdit.withDocumentId(edit.get("id").asText());
            eb.clientId(edit.get("clientId").asText());
            eb.clientVersion(edit.get("clientVersion").asLong());
            eb.serverVersion(edit.get("serverVersion").asLong());
            eb.checksum(edit.get("checksum").asText());
            final JsonNode diffsNode = edit.get("diffs");
            if (diffsNode.isArray()) {
                for (JsonNode d : diffsNode) {
                    final JsonPatch patch = om.readValue(d.get("patch").asText(), JsonPatch.class);
                    eb.diff(patch);
                }
            }
            return eb.build();
        }
    }

    private static class EditSerializer extends JsonSerializer<JsonPatchEdit> {

        @Override
        public void serialize(final JsonPatchEdit edit,
                              final JsonGenerator jgen,
                              final SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("msgType", "patch");
            jgen.writeStringField("clientId", edit.clientId());
            jgen.writeStringField("id", edit.documentId());
            jgen.writeNumberField("clientVersion", edit.clientVersion());
            jgen.writeNumberField("serverVersion", edit.serverVersion());
            jgen.writeStringField("checksum", edit.checksum());
            jgen.writeArrayFieldStart("diffs");
            if (!edit.diffs().isEmpty()) {
                for (JsonPatchDiff diff : edit.diffs()) {
                    jgen.writeArrayFieldStart("patch");
                    diff.jsonPatch().serialize(jgen, provider);
                    jgen.writeEndArray();
                }
            }
            jgen.writeEndArray();
        }
    }
}

