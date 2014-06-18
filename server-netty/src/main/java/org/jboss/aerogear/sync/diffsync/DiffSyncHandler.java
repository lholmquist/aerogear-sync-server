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
package org.jboss.aerogear.sync.diffsync;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.aerogear.sync.JsonMapper;
import org.jboss.aerogear.sync.diffsync.server.MessageType;
import org.jboss.aerogear.sync.diffsync.server.ServerSyncEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class DiffSyncHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(DiffSyncHandler.class);

    private static final ConcurrentHashMap<String, Set<ChannelHandlerContext>> clients =
            new ConcurrentHashMap<String, Set<ChannelHandlerContext>>();
    private final ServerSyncEngine<String> syncEngine;

    public DiffSyncHandler(final ServerSyncEngine<String> syncEngine) {
        this.syncEngine = syncEngine;
    }

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, final WebSocketFrame frame) throws Exception {
        if (frame instanceof CloseWebSocketFrame) {
            logger.debug("Received closeFrame");
            ctx.close();
        }
        else if (frame instanceof TextWebSocketFrame) {
            final JsonNode json = JsonMapper.asJsonNode(((TextWebSocketFrame) frame).text());
            logger.info(json.toString());
            switch (MessageType.from(json.get("msgType").asText())) {
            case ADD:
                final Document<String> doc = documentFromJson(json);
                addDocument(doc, json.get("clientId").asText());
                addClientListener(doc.id(), ctx);
                respond(ctx, "ADDED");
                break;
            case EDITS:
                final Edits clientEdits = JsonMapper.fromJson(json.toString(), Edits.class);
                final Edits edits = patch(clientEdits);
                respond(ctx, "PATCHED");
                notifyClientListeners(edits);
            case DETACH:
                // detach the client from a specific document.
            case UNKNOWN:
                ctx.channel().writeAndFlush(textFrame("{\"result\": \"Unknown msgType '" + json.get("msgType").asText() + "'\"}"));
            }
        }
    }

    private void addDocument(final Document<String> document, final String clientId) {
        syncEngine.addDocument(document, clientId);
    }

    private Edits patch(final Edits clientEdits) {
        return syncEngine.patch(clientEdits);
    }

    private static Document<String> documentFromJson(final JsonNode json) {
        return new DefaultDocument<String>(json.get("docId").asText(), json.get("content").asText());
    }

    private static void addClientListener(final String documentId, final ChannelHandlerContext ctx) {
        if (!clients.containsKey(documentId)) {
            final Set<ChannelHandlerContext> contexts = new HashSet<ChannelHandlerContext>();
            contexts.add(ctx);
            clients.put(documentId, contexts);
        } else {
            synchronized (clients) {
                final Set<ChannelHandlerContext> contexts = clients.get(documentId);
                contexts.add(ctx);
            }
        }
    }

    private static void notifyClientListeners(final Edits edits) {
        final Set<ChannelHandlerContext> contexts = clients.get(edits.documentId());
        for (ChannelHandlerContext ctx : contexts) {
            ctx.channel().writeAndFlush(textFrame(JsonMapper.toJson(edits)));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Caught exception", cause);
        ctx.channel().writeAndFlush(textFrame("{\"result\": \"" + cause.getMessage() + "\"}"));
    }

    private static void respond(final ChannelHandlerContext ctx, final String msg) {
        final TextWebSocketFrame textWebSocketFrame = textFrame("{\"result\": \"" + msg + "\"}");
        logger.info("Responding: " + textWebSocketFrame.text());
        ctx.channel().writeAndFlush(textWebSocketFrame);
    }

    private static TextWebSocketFrame textFrame(final String text) {
        return new TextWebSocketFrame(text);
    }
}