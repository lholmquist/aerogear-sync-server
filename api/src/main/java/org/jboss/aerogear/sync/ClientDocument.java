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

/**
 * A client document is used on both the server and client side and
 * associates a client identifier with a {@link Document}.
 *
 * @param <T> the type of this documents content.
 */
public interface ClientDocument<T> extends Document<T> {

    /**
     * Identifies a client or session to which this Document belongs.
     *
     * @return {@code String} the client identifier.
     */
    String clientId();
}
