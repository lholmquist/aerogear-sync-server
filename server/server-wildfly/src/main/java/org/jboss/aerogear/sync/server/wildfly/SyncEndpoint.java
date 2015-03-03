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
package org.jboss.aerogear.sync.server.wildfly;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.logging.Logger;
import javax.websocket.CloseReason;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.OnClose;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.diffmatchpatch.JsonMapper;
import org.jboss.aerogear.sync.jsonpatch.JsonPatchEdit;
import org.jboss.aerogear.sync.jsonpatch.server.JsonPatchServerSynchronizer;
import org.jboss.aerogear.sync.server.MessageType;
import org.jboss.aerogear.sync.server.ServerInMemoryDataStore;
import org.jboss.aerogear.sync.server.ServerSyncEngine;
import org.jboss.aerogear.sync.server.Subscriber;

@ServerEndpoint("/sync")
public class SyncEndpoint {

    private static final JsonPatchServerSynchronizer synchronizer = new JsonPatchServerSynchronizer();
    private static final ServerInMemoryDataStore<JsonNode, JsonPatchEdit> dataStore = new ServerInMemoryDataStore<JsonNode, JsonPatchEdit>();
    private static final ServerSyncEngine<JsonNode, JsonPatchEdit> syncEngine = new ServerSyncEngine<JsonNode, JsonPatchEdit>(synchronizer, dataStore);
    private static final String DOC_ADD = "DOC_ADD";
    private static final String WILDFLY_SUBSCRIBER = "WILDFLY_SUBSCRIBER";
    private static final Logger logger = Logger.getLogger(SyncEndpoint.class.getSimpleName());
    private static final String DOCUMENT_ID = "DOCUMENT_ID";
    
    @OnMessage
    public String onMessage(String message, Session webSocketSession) {
        final JsonNode json = JsonMapper.asJsonNode(message);

        switch (MessageType.from(json.get("msgType").asText())) {
            case ADD:
                final Document<JsonNode> doc = syncEngine.documentFromJson(json);
                final String clientId = json.get("clientId").asText();
                final PatchMessage<JsonPatchEdit> patchMessage = addSubscriber(doc, clientId, webSocketSession);
                webSocketSession.getUserProperties().put(DOC_ADD, true);
                return (patchMessage.asJson());

            case PATCH:
                final PatchMessage<JsonPatchEdit> clientPatchMessage = syncEngine.patchMessageFromJson(json.toString());
                checkForReconnect(clientPatchMessage.documentId(), clientPatchMessage.clientId(), webSocketSession);
                patch(clientPatchMessage);
                break;
            case DETACH:
                // detach the client from a specific document.
                break;
            case UNKNOWN:
                return "{\"result\": \"Unknown msgType '" + json.get("msgType").asText() + "'\"}";
        
        }
        
        return message;
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("WebSocket opened: " + session.getId());
    }

    @OnClose
    public void onClose(CloseReason reason, Session webSocketSession) {
        logger.info("Closing a WebSocket due to " + reason.getReasonPhrase());
        WildflySubscriber subscriber = (WildflySubscriber) webSocketSession.getUserProperties().get(WILDFLY_SUBSCRIBER);
        String documentId = (String) webSocketSession.getUserProperties().get(DOCUMENT_ID);
        syncEngine.removeSubscriber(subscriber, documentId);

    }

    private PatchMessage<JsonPatchEdit> addSubscriber(final Document<JsonNode> document,
        final String clientId, Session session) {
        final Subscriber<Session> subscriber = new WildflySubscriber(clientId, session);
        return syncEngine.addSubscriber(subscriber, document);
    }

    private void patch(final PatchMessage<JsonPatchEdit> patchMessage) {
        syncEngine.notifySubscribers(syncEngine.patch(patchMessage));
    }

    private void checkForReconnect(final String documentId, final String clientId, final Session session) {
        if (getOrDefault(session.getUserProperties(),Boolean.FALSE) == Boolean.TRUE) {
            return;
        }
        logger.info("Reconnected client [" + clientId + "]. Adding as listener.");

        final WildflySubscriber subscriber = new WildflySubscriber(clientId, session);
        syncEngine.connectSubscriber(subscriber, documentId);
    }

    private static Boolean getOrDefault(final Map<String, Object> properties, final Boolean defaultValue) {
        final Boolean value = (Boolean) properties.get(DOC_ADD);
        return value != null ? value : defaultValue;
    }

}
