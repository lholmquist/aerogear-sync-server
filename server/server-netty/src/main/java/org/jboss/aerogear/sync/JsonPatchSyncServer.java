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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.jboss.aerogear.sync.jsonpatch.JsonPatchEdit;
import org.jboss.aerogear.sync.jsonpatch.server.JsonPatchServerSynchronizer;
import org.jboss.aerogear.sync.server.ServerInMemoryDataStore;
import org.jboss.aerogear.sync.server.ServerSyncEngine;

import java.util.concurrent.Executors;

/**
 * A Netty based WebSocket server that is able to handle differential synchronization edits.
 */
public final class JsonPatchSyncServer {

    private static final String DEFAULT_CONFIG = "/sync.config";

    public static void main(final String... args) throws Exception {
        final String configFile = args.length == 0 ? DEFAULT_CONFIG : args[0];
        final StandaloneConfig config = ConfigReader.parse(configFile);
        final EventLoopGroup bossGroup = new NioEventLoopGroup();
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        final JsonPatchServerSynchronizer synchronizer = new JsonPatchServerSynchronizer();
        final ServerInMemoryDataStore<JsonNode, JsonPatchEdit> dataStore = new ServerInMemoryDataStore<JsonNode, JsonPatchEdit>();
        final ServerSyncEngine<JsonNode, JsonPatchEdit> syncEngine = new ServerSyncEngine<JsonNode, JsonPatchEdit>(synchronizer, dataStore);
        final DiffSyncHandler<JsonNode, JsonPatchEdit> diffSyncHandler = new DiffSyncHandler<JsonNode, JsonPatchEdit>(syncEngine);
        try {
            final ServerBootstrap sb = new ServerBootstrap();
            sb.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(final SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new HttpRequestDecoder(),
                                    new HttpObjectAggregator(65536),
                                    new HttpResponseEncoder(),
                                    new WebSocketServerProtocolHandler("/sync"),
                                    diffSyncHandler);
                        }
                    });
            if (config.isGcmEnabled()) {
                sb.handler(new GcmHandler<JsonNode, JsonPatchEdit>(config, syncEngine, Executors.newSingleThreadExecutor()));
            }

            final Channel ch = sb.bind(config.host(), config.port()).sync().channel();
            System.out.println("JsonPatchSyncServer bound to " + config.host() + ':' + config.port());

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
