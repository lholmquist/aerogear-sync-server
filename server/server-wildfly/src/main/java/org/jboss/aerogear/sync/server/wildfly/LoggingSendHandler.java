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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;

/**
 * An implementation of the SendHandler callback object that logs any errors that occured after the websocket message 
 * has been transmitted.
 */ 
public class LoggingSendHandler implements SendHandler{
    public final static SendHandler INSTANCE = new LoggingSendHandler();

    private LoggingSendHandler(){}
    
    @Override
    public void onResult(SendResult result) {
        if (!result.isOK()) {
            Throwable error = result.getException();
            Logger.getAnonymousLogger().log(Level.SEVERE, error.getMessage(), error);
        }
    }
}
