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
import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient.Response;

public class BasicResponse implements Document<BasicResponse> {
    private final DocNode content;
    private final String eTag;

    public BasicResponse(DocNode content) {
        this.content = content;
        this.eTag = null;
    }

    public BasicResponse(Response content) throws InvalidResponseException {
        this.content = content.asDocNode();
        this.eTag = content.getETag();
    }

    public String getMessage() {
        return content.getAsString("message");
    }

    @Override
    public String toString() {
        return content.toJsonString();
    }

    public Map<String, Object> getContent() {
        return content;
    }

    @Override
    public Object toBasicObject() {
        return content.toBasicObject();
    }

    public String getETag() {
        return eTag;
    }
}
