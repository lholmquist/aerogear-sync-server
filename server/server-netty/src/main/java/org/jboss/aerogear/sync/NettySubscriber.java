package org.jboss.aerogear.sync;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.aerogear.sync.server.Subscriber;


public class NettySubscriber implements Subscriber<ChannelHandlerContext> {

    private final String clientId;
    private final ChannelHandlerContext ctx;

    public NettySubscriber(final String clientId, final ChannelHandlerContext ctx) {
        this.clientId = clientId;
        this.ctx = ctx;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public ChannelHandlerContext channel() {
        return ctx;
    }

    @Override
    public void patched(final PatchMessage<?> patchMessage) {
        ctx.channel().writeAndFlush(textFrame(patchMessage.asJson()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        final Subscriber<ChannelHandlerContext> subscriber = (Subscriber<ChannelHandlerContext>) o;
        if (!clientId.equals(subscriber.clientId())) {
            return false;
        }
        return !ctx.equals(subscriber.channel());
    }

    @Override
    public int hashCode() {
        int result = clientId.hashCode();
        result = 31 * result + ctx.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Client[clientId=" + clientId + ", channel=" + ctx + ']';
    }

    private static TextWebSocketFrame textFrame(final String text) {
        return new TextWebSocketFrame(text);
    }

}
