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

package com.floragunn.searchguard.sgctl.client.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;

public class GetUserResponse {

    private String eTag;
    private final String description;
    private final List<String> searchGuardRoles;
    private final List<String> backendRoles;
    private final Map<String, Object> attributes;
    private final DocNode docNode;

    public GetUserResponse(DocNode docNode) throws InvalidResponseException {
        this.docNode = docNode;

        DocNode dataNode = docNode.getAsNode("data");

        if (dataNode.isNull()) {
            throw new InvalidResponseException("data element is missing in response");
        }

        this.description = dataNode.getAsString("description");
        this.searchGuardRoles = dataNode.getAsListOfStrings("search_guard_roles");
        this.backendRoles = dataNode.getAsListOfStrings("backend_roles");
        this.attributes = dataNode.hasNonNull("attributes") ? dataNode.getAsNode("attributes").toNormalizedMap() : Collections.emptyMap();
    }

    public GetUserResponse(SearchGuardRestClient.Response response) throws InvalidResponseException {
        this(response.asDocNode());
        this.eTag = response.getETag();
    }

    public String getETag() {
        return eTag;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getSearchGuardRoles() {
        return searchGuardRoles;
    }

    public List<String> getBackendRoles() {
        return backendRoles;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public DocNode getDocNode() {
        return docNode;
    }
}
