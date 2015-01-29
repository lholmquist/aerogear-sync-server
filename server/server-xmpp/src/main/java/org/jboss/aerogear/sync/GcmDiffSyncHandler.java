package org.jboss.aerogear.sync;

import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchEdit;
import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchMessage;
import org.jboss.aerogear.sync.diffmatchpatch.JsonMapper;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.sync.server.MessageType;
import org.jboss.aerogear.sync.server.ServerSyncEngine;
import org.jivesoftware.smack.PacketListener;

import static org.jboss.aerogear.sync.GcmMessages.createJsonMessage;
import static org.jboss.aerogear.sync.diffmatchpatch.JsonMapper.fromJson;
import static org.jboss.aerogear.sync.diffmatchpatch.JsonMapper.toJson;

public class GcmDiffSyncHandler implements PacketListener {

    private static final Logger logger = Logger.getLogger(GcmDiffSyncHandler.class.getCanonicalName());
    private static final String GCM_ELEMENT_NAME = "gcm";
    private static final String GCM_NAMESPACE = "google:mobile:data";
    private XMPPConnection connection;
    private final ServerSyncEngine<String, DiffMatchPatchEdit> syncEngine;

    /**
     * Indicates whether the connection is in draining state, which means that
     * it will not accept any new downstream messages.
     */
    protected volatile boolean connectionDraining;

    static {
        ProviderManager.addExtensionProvider(GCM_ELEMENT_NAME, GCM_NAMESPACE,
                new PacketExtensionProvider() {
                    @Override
                    public PacketExtension parseExtension(XmlPullParser parser) throws
                            Exception {
                        String json = parser.nextText();
                        return new GcmPacketExtension(json);
                    }
                });
    }

    public GcmDiffSyncHandler(final ServerSyncEngine<String, DiffMatchPatchEdit> syncEngine, XMPPConnection connection) {
        this.connection = connection;
        this.syncEngine = syncEngine;
    }

    /**
     * Handles an ACK.
     *
     * <p>
     * Logs a INFO message, but subclasses could override it to properly handle
     * ACKs.
     *
     * @param jsonObject the messageObject
     */
    protected void handleAckReceipt(JsonNode jsonObject) {
        String messageId = jsonObject.get("message_id").asText();
        String from = jsonObject.get("from").asText();
        logger.log(Level.INFO, "handleAckReceipt() from: " + from + ",messageId: " + messageId);
    }

    /**
     * Handles a NACK.
     *
     * <p>
     * Logs a INFO message, but subclasses could override it to properly handle
     * NACKs.
     *
     * @param jsonObject the messageObject
     */
    protected void handleNackReceipt(JsonNode jsonObject) {
        String messageId = jsonObject.get("message_id").asText();
        String from = jsonObject.get("from").asText().replace("\"", "");
        logger.log(Level.INFO, "handleNackReceipt() from: " + from + ",messageId: " + messageId);
    }

    protected void handleControlMessage(JsonNode jsonObject) {
        logger.log(Level.INFO, "handleControlMessage(): " + jsonObject);
        String controlType = jsonObject.get("control_type").asText();
        if ("CONNECTION_DRAINING".equals(controlType)) {
            connectionDraining = true;
        } else {
            logger.log(Level.INFO, "Unrecognized control type: %s. This could happen if new features are " + "added to the CCS protocol.", controlType);
        }
    }

    /**
     * Creates a JSON encoded ACK message for an upstream message received from
     * an application.
     *
     * @param to RegistrationId of the device who sent the upstream message.
     * @param messageId messageId of the upstream message to be acknowledged to
     * CCS.
     * @return JSON encoded ack.
     */
    protected static String createJsonAck(String to, String messageId) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("message_type", "ack");
        message.put("to", to);
        message.put("message_id", messageId);
        return JSONValue.toJSONString(message);
    }

    @Override
    public void processPacket(Packet packet) {
        logger.log(Level.INFO, "Received: " + packet.toXML());
        Message incomingMessage = (Message) packet;
        GcmPacketExtension gcmPacket
                = (GcmPacketExtension) incomingMessage.
                getExtension(GCM_NAMESPACE);
        String body = gcmPacket.getJson();
        try {
            @SuppressWarnings("unchecked")
            JsonNode jsonObject = JsonMapper.asJsonNode(body);

            // present for "ack"/"nack", null otherwise
            JsonNode messageType = jsonObject.get("message_type");

            if (messageType == null) {
                // Normal upstream data message
                messageReceived(jsonObject);

                // Send ACK to CCS
                String messageId = jsonObject.get("message_id").asText();
                String clientId = jsonObject.get("from").asText().replace("\"", "");
                String ack = createJsonAck(clientId, messageId);
                send(ack);
            } else if ("ack".equals(messageType.asText())) {
                // Process Ack
                handleAckReceipt(jsonObject);
            } else if ("nack".equals(messageType.asText())) {
                // Process Nack
                handleNackReceipt(jsonObject);
            } else if ("control".equals(messageType.asText())) {
                // Process control message
                handleControlMessage(jsonObject);
            } else {
                logger.log(Level.WARNING,
                        "Unrecognized message type: "+
                                messageType.asText());
            }
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "Error parsing JSON " + body, e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to process packet", e);
        }
    }

    protected void messageReceived(JsonNode json) throws Exception {

        JsonNode syncMessage = JsonMapper.asJsonNode(json.get("data").get("message").asText());
        logger.info("Doc:" + json);
        final String googleRegistrationId = json.get("from").asText();
        final String diffsyncClientId = clientIdFromJson(syncMessage);
        switch (MessageType.from(syncMessage.get("msgType").asText())) {
            case ADD:
                final Document<String> doc = documentFromJson(syncMessage);
                final PatchMessage<DiffMatchPatchEdit> patchMessage = addSubscriber(doc, diffsyncClientId, googleRegistrationId);
                send(createJsonMessage(googleRegistrationId, "m-" + UUID.randomUUID(), toJson(patchMessage)));
                break;
            case PATCH:
                final PatchMessage<DiffMatchPatchEdit> clientPatchMessage = fromJson(syncMessage.toString(), DiffMatchPatchMessage.class);
                checkForReconnect(clientPatchMessage.documentId(), googleRegistrationId, diffsyncClientId);
                logger.log(Level.FINER, "Client Edits=" + clientPatchMessage);
                patch(clientPatchMessage);
                break;
            case DETACH:
                // detach the client from a specific document.
                break;
            case UNKNOWN:
                //unknownMessageType(ctx, json);
                break;
        }
    }

    private PatchMessage<DiffMatchPatchEdit> addSubscriber(final Document<String> document,
                                       final String clientId,
                                       final String googleRegistrationId) {
        final GcmSubscriber gcmSubscriber = new GcmSubscriber(clientId, googleRegistrationId, connection);
        return syncEngine.addSubscriber(gcmSubscriber, document);
    }

    private void patch(final PatchMessage<DiffMatchPatchEdit> clientEdit) {
        syncEngine.patchAndNotifySubscribers(clientEdit);
    }

    /**
     * Sends a packet with contents provided.
     */
    protected void send(String jsonRequest) throws SmackException.NotConnectedException {
        Packet request = new GcmPacketExtension(jsonRequest).toPacket();
        connection.sendPacket(request);
    }

    private static Document<String> documentFromJson(final JsonNode json) {
        final JsonNode contentNode = json.get("content");
        String content = null;
        if (contentNode != null && !contentNode.isNull()) {
            if (contentNode.isArray() || contentNode.isObject()) {
                content = JsonMapper.toString(contentNode);
            } else {
                content = contentNode.asText();
            }
        }
        return new DefaultDocument<String>(json.get("id").asText(), content);
    }

    private void checkForReconnect(final String documentId, final String registrationId, final String clientId) {
        logger.info("Reconnected client [" + registrationId + "]. Adding as listener.");
        // the context was used to reconnect so we need to add client as a listener
        final GcmSubscriber gcmSubscriber = new GcmSubscriber(clientId, registrationId, connection);
        syncEngine.addSubscriber(gcmSubscriber, documentId);
    }

    private static String clientIdFromJson(JsonNode syncMessage) {
        final JsonNode clientId = syncMessage.get("clientId");
        String content = null;
        if (clientId != null && !clientId.isNull()) {
            content = clientId.asText();

        }
        return content;
    }

    /**
     * XMPP Packet Extension for GCM Cloud Connection Server.
     */
    static final class GcmPacketExtension extends DefaultPacketExtension {

        private final String json;

        GcmPacketExtension(String json) {
            super(GCM_ELEMENT_NAME, GCM_NAMESPACE);
            this.json = json;
        }

        public String getJson() {
            return json;
        }

        @Override
        public String toXML() {
            return String.format("<%s xmlns=\"%s\">%s</%s>",
                    GCM_ELEMENT_NAME, GCM_NAMESPACE,
                    StringUtils.escapeForXML(json), GCM_ELEMENT_NAME);
        }

        public Packet toPacket() {
            Message message = new Message();
            message.addExtension(this);
            return message;
        }
    }

}
