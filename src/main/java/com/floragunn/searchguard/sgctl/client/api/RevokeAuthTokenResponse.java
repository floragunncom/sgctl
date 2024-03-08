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

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class RevokeAuthTokenResponse {

    private final Map<String, Object> content;

    public RevokeAuthTokenResponse(DocNode content)  {
        this.content = content;
    }

    public RevokeAuthTokenResponse(SearchGuardRestClient.Response response) throws InvalidResponseException {
        this(response.asDocNode());
    }

    public String getErrors() {
        return getAsString("error");
    }
    public String getStatus() {
        return getAsString("status");
    }

    protected String getAsString(String name) {
        return content.get(name) != null ? String.valueOf(content.get(name)) : null;
    }
    public String toString() {
        return getStatus() + " " + getErrors();
    }

    }
