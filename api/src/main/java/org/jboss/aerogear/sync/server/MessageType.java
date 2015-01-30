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
package org.jboss.aerogear.sync.server;

import java.util.HashMap;
import java.util.Map;

public enum MessageType {
    ADD,
    PATCH,
    DETACH,
    UNKNOWN;

    private static final Map<String, MessageType> MAP;
    static {
        final Map<String, MessageType> map = new HashMap<String, MessageType>();
        for (MessageType type : values()) {
            final String name = type.name();
            if (name != null) {
                map.put(name, type);
            }
        }
        MAP = map;
    }

    public static MessageType from(final String name) {
        final MessageType type = MAP.get(name.toUpperCase());
        return type == null ? UNKNOWN : type;
    }
}
