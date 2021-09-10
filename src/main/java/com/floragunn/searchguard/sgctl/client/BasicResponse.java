/*
 * Copyright 2021 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.floragunn.searchguard.sgctl.client;

import java.util.Map;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;

public class BasicResponse implements Document {
    private final Map<String, Object> content;

    public BasicResponse(Map<String, Object> content) {
        this.content = content;
    }
    
    public BasicResponse(DocNode content) {
        this.content = content;
    }
    
    public String getMessage() {
        return getAsString("message");
    }

    protected String getAsString(String name) {
        return content.get(name) != null ? String.valueOf(content.get(name)) : null;
    }

    @Override
    public String toString() {
        return DocWriter.json().writeAsString(content);
    }

    public Map<String, Object> getContent() {
        return content;
    }

    @Override
    public Map<String, Object> toMap() {
        return content;
    }
}
