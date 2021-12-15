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

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

public class PreconditionFailedException extends ApiException {

    private static final long serialVersionUID = -8533002682306022274L;

    public PreconditionFailedException(String message, StatusLine statusLine, HttpResponse httpResponse, String httpResponseBody) {
        super(message, statusLine, httpResponse, httpResponseBody);
    }

    public PreconditionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreconditionFailedException(String message) {
        super(message);
    }

}
