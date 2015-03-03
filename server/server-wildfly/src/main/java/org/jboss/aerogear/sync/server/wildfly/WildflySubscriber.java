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

import javax.websocket.Session;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.server.Subscriber;

/**
 * Represents a subscriber of patches for the WebSocket Session type.
 */
public class WildflySubscriber implements Subscriber<Session> {
    private final Session webSocketSession;
    private final String clientId;

    public WildflySubscriber(String clientId, Session webSocketSession) {
        this.clientId = clientId;
        this.webSocketSession = webSocketSession;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public Session channel() {
        return webSocketSession;
    }

    @Override
    public void patched(PatchMessage<?> patchMessage) {
        if (webSocketSession.isOpen()) {
            webSocketSession.getAsyncRemote().sendText(patchMessage.asJson(), LoggingSendHandler.INSTANCE);
        }
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
        final Subscriber<Session> subscriber = (Subscriber<Session>) o;
        if (!clientId.equals(subscriber.clientId())) {
            return false;
        }
        return !webSocketSession.equals(subscriber.channel());
    }

    @Override
    public int hashCode() {
        int result = clientId.hashCode();
        result = 31 * result + webSocketSession.hashCode();
        return result;
    }
    
}
