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
package org.jboss.aerogear.sync.jsonmergepatch;

import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.jboss.aerogear.sync.Diff;

public class JsonMergePatchDiff implements Diff {

    private final JsonMergePatch jsonMergePatch;

    public JsonMergePatchDiff(final JsonMergePatch jsonMergePatch) {
        this.jsonMergePatch = jsonMergePatch;
    }

    public JsonMergePatch jsonMergePatch() {
        return jsonMergePatch;
    }

    @Override
    public String toString() {
        return "JsonMergePatchDiff[jsonMergePatch=" + jsonMergePatch + ']';
    }
}
