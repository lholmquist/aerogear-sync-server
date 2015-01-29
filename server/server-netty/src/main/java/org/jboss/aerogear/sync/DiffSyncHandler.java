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
package org.jboss.aerogear.sync;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;
import org.jboss.aerogear.sync.diffmatchpatch.JsonMapper;
import org.jboss.aerogear.sync.server.MessageType;
import org.jboss.aerogear.sync.server.ServerSyncEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.aerogear.sync.diffmatchpatch.JsonMapper.toJson;

@ChannelHandler.Sharable
public class DiffSyncHandler<T, S extends Edit> extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(DiffSyncHandler.class);
    private static final AttributeKey<Boolean> DOC_ADD = AttributeKey.valueOf(DiffSyncHandler.class, "DOC_ADD");

    private final ServerSyncEngine<T, S> syncEngine;

    public DiffSyncHandler(final ServerSyncEngine<T, S> syncEngine) {
        this.syncEngine = syncEngine;
    }

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, final WebSocketFrame frame) throws Exception {
        if (frame instanceof CloseWebSocketFrame) {
            logger.debug("Received closeFrame");
            ctx.close();
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            final JsonNode json = JsonMapper.asJsonNode(((TextWebSocketFrame) frame).text());
            logger.info("Doc:" + json);
            switch (MessageType.from(json.get("msgType").asText())) {
            case ADD:
                final Document<T> doc = syncEngine.documentFromJson(json);
                final String clientId = json.get("clientId").asText();
                final PatchMessage<S> patchMessage = addSubscriber(doc, clientId, ctx);
                ctx.attr(DOC_ADD).set(true);
                ctx.channel().writeAndFlush(textFrame(toJson(patchMessage)));
                break;
            case PATCH:
                final PatchMessage<S> clientPatchMessage = syncEngine.patchMessagefromJson(json.toString());
                checkForReconnect(clientPatchMessage.documentId(), clientPatchMessage.clientId(), ctx);
                logger.debug("Client Edits=" + clientPatchMessage);
                patch(clientPatchMessage);
                break;
            case DETACH:
                // detach the client from a specific document.
                break;
            case UNKNOWN:
                unknownMessageType(ctx, json);
                break;
            }
        } else {
            ctx.fireChannelRead(frame);
        }
    }

    private PatchMessage<S> addSubscriber(final Document<T> document,
                                       final String clientId,
                                       final ChannelHandlerContext ctx) {
        final NettySubscriber subscriber = new NettySubscriber(clientId, ctx);
        addCloseHandler(ctx, subscriber, document.id());
        return syncEngine.addSubscriber(subscriber, document);
    }

    private void patch(final PatchMessage<S> clientEdit) {
        syncEngine.patchAndNotifySubscribers(clientEdit);
    }

    private void checkForReconnect(final String documentId, final String clientId, final ChannelHandlerContext ctx) {
        if (ctx.attr(DOC_ADD).get() == Boolean.TRUE) {
            return;
        }
        logger.info("Reconnected client [" + clientId + "]. Adding as listener.");
        // the context was used to reconnect so we need to add client as a listener
        addSubscriber(ctx, clientId, documentId);
    }

    private void addSubscriber(final ChannelHandlerContext ctx, final String clientId, final String documentId) {
        final NettySubscriber subscriber = new NettySubscriber(clientId, ctx);
        syncEngine.addSubscriber(subscriber, documentId);
        addCloseHandler(ctx, subscriber, documentId);
    }

    private void addCloseHandler(final ChannelHandlerContext ctx,
                                 final NettySubscriber subscriber,
                                 final String documentId) {
        ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                syncEngine.removeSubscriber(subscriber, documentId);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Caught exception", cause);
    }

    private static void unknownMessageType(final ChannelHandlerContext ctx, final JsonNode json) {
        ctx.channel().writeAndFlush(textFrame("{\"result\": \"Unknown msgType '" + json.get("msgType").asText() + "'\"}"));
    }

    private static TextWebSocketFrame textFrame(final String text) {
        return new TextWebSocketFrame(text);
    }

}
