package org.jboss.aerogear.sync;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.sync.diffmatchpatch.JsonMapper;
import org.json.simple.JSONValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class GcmMessages {

    private GcmMessages() {
    }

    /**
     * Creates a JSON encoded GCM message.
     *
     * @param to RegistrationId of the target device (Required).
     * @param messageId Unique messageId for which CCS will send an "ack/nack" (Required).
     * @param payload Message content intended for the application. (Optional).
     * @return JSON encoded GCM message.
     */
    public static String createJsonMessage(String to,
                                           String messageId,
                                           String payload) {
        return createJsonMessage(to, messageId, payload, null, null, null);
    }

    /**
     * Creates a JSON encoded GCM message.
     *
     * @param to RegistrationId of the target device (Required).
     * @param messageId Unique messageId for which CCS will send an "ack/nack"
     * (Required).
     * @param payload Message content intended for the application. (Optional).
     * @param collapseKey GCM collapse_key parameter (Optional).
     * @param timeToLive GCM time_to_live parameter (Optional).
     * @param delayWhileIdle GCM delay_while_idle parameter (Optional).
     * @return JSON encoded GCM message.
     */
    public static String createJsonMessage(String to, String messageId,
                                           String payload, String collapseKey, Long timeToLive,
                                           Boolean delayWhileIdle) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("to", to);
        if (collapseKey != null) {
            message.put("collapse_key", collapseKey);
        }
        if (timeToLive != null) {
            message.put("time_to_live", timeToLive);
        }
        if (delayWhileIdle != null && delayWhileIdle) {
            message.put("delay_while_idle", true);
        }
        message.put("message_id", messageId);
        message.put("data", payloadToMap(payload));
        return JSONValue.toJSONString(message);
    }


    private static Map<String, String> payloadToMap(String payload) {
        Map<String, String> data = new HashMap<String, String>();
        JsonNode payloadJson = JsonMapper.asJsonNode(payload);
        Iterator<Entry<String, JsonNode>> fields = payloadJson.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isValueNode()) {
                data.put(field.getKey(), field.getValue().asText());
            } else {
                data.put(field.getKey(), JsonMapper.toString(field.getValue()));
            }
        }
        return data;
    }

}
