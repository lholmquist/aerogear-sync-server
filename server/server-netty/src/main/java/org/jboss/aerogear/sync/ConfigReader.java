/**
 * JBoss, Home of Professional Open Source Copyright Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.jboss.aerogear.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.aerogear.sync.StandaloneConfig.Builder;

/**
 * Utility to read a JSON config files.
 */
public final class ConfigReader {

    private static final ObjectMapper OM = new ObjectMapper();

    private ConfigReader() {
    }

    /**
     * Will parse the passed in file, which can either be a file on the file system
     * or a file on the classpath into a {@link StandaloneConfig} instance.
     *
     * @param fileName the name of a file on the file system or on the classpath.
     * @return {@link StandaloneConfig} populated with the values in the JSON configuration file.
     * @throws Exception
     */
    public static StandaloneConfig parse(final String fileName) throws Exception {
        final File configFile = new File(fileName);
        InputStream in = null;
        try {
            in = configFile.exists() ? new FileInputStream(configFile) : ConfigReader.class.getResourceAsStream(fileName);
            return parse(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Will parse the passed InputStream into a {@link StandaloneConfig} instance.
     *
     * @param in the input stream to parse. Should be from a JSON source representing a SimplePush configuration.
     * @return {@link StandaloneConfig} populated with the values in the JSON input stream.
     */
    public static StandaloneConfig parse(final InputStream in) {
        try {
            final JsonNode json = OM.readTree(in);
            return parseProperties(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static StandaloneConfig parseProperties(final JsonNode json) {
        final Builder b = StandaloneConfig.host(json.get("host").asText());
        b.port(json.get("port").asInt());

        final JsonNode gcm = json.get("gcm");
        if (gcm != null) {
            final JsonNode enabled = gcm.get("enabled");
            if (enabled != null && enabled.asBoolean()){
                b.gcmEnabled();
            }

            final JsonNode gcmHost = gcm.get("host");
            if (gcmHost != null) {
                b.gcmHost(gcmHost.asText());
            }
            final JsonNode gcmPort = gcm.get("port");
            if (gcmPort != null) {
                b.gcmPort(gcmPort.asInt());
            }
            final JsonNode gcmSenderId = gcm.get("senderId");
            if (gcmSenderId != null) {
                b.gcmSenderId(gcmSenderId.asLong());
            }
            final JsonNode gcmApiKey = gcm.get("apiKey");
            if (gcmApiKey != null) {
                b.gcmApiKey(gcmApiKey.asText());
            }
        }
        return b.build();
    }

}
