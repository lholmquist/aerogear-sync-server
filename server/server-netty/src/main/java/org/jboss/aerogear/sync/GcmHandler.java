package org.jboss.aerogear.sync;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchEdit;
import org.jboss.aerogear.sync.server.ServerSyncEngine;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class GcmHandler extends ChannelHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcmHandler.class);

    private final StandaloneConfig syncConfig;
    private final ServerSyncEngine<String, DiffMatchPatchEdit> syncEngine;
    private final ExecutorService executorService;

    private XMPPConnection connection;

    public GcmHandler(final StandaloneConfig syncConfig,
                      final ServerSyncEngine<String, DiffMatchPatchEdit> syncEngine,
                      final ExecutorService executorService) {
        this.syncConfig = syncConfig;
        this.syncEngine = syncEngine;
        this.executorService = executorService;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                LOGGER.info("handler GcmHandler: " + syncConfig);
                connect();
                return null;
            }
        });
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        LOGGER.info("Removing GcmHandler");
        connection.disconnect();
        executorService.shutdown();
    }

    public void connect() throws XMPPException, IOException, SmackException {
        final ConnectionConfiguration config = new ConnectionConfiguration(syncConfig.gcmHost(), syncConfig.gcmPort());
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
        config.setReconnectionAllowed(true);
        config.setRosterLoadedAtLogin(false);
        config.setDebuggerEnabled(false);
        config.setSendPresence(false);
        config.setSocketFactory(SSLSocketFactory.getDefault());

        connection = new XMPPTCPConnection(config);
        connection.addConnectionListener(new LoggingConnectionListener());

        // Handle incoming packets
        connection.addPacketListener(new GcmDiffSyncHandler(syncEngine, connection), new PacketTypeFilter(Message.class));

        // Log all outgoing packets
        connection.addPacketInterceptor(new PacketInterceptor() {
            @Override
            public void interceptPacket(Packet packet) {
                LOGGER.info("Sent: {}", packet.toXML());
            }
        }, new PacketTypeFilter(Message.class));

        connection.connect();
        connection.login(syncConfig.gcmSenderId() + "@gcm.googleapis.com", syncConfig.gcmApiKey());
    }

    private static final class LoggingConnectionListener implements ConnectionListener {

        @Override
        public void connected(XMPPConnection xmppConnection) {
            LOGGER.info("Connected.");
        }

        @Override
        public void authenticated(XMPPConnection xmppConnection) {
            LOGGER.info("Authenticated.");
        }

        @Override
        public void reconnectionSuccessful() {
            LOGGER.info("Reconnecting..");
        }

        @Override
        public void reconnectionFailed(Exception e) {
            LOGGER.info("Reconnection failed.. ", e);
        }

        @Override
        public void reconnectingIn(int seconds) {
            LOGGER.info("Reconnecting in {} secs", seconds);
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            LOGGER.info("Connection closed on error.");
        }

        @Override
        public void connectionClosed() {
            LOGGER.info("Connection closed.");
        }
    }

}
