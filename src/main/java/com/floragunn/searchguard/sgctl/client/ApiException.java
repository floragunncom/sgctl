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

import com.floragunn.codova.validation.ValidationErrors;

public class ApiException extends Exception {

    private static final long serialVersionUID = -2613151852034285098L;
    private final StatusLine statusLine;
    private final HttpResponse httpResponse;
    private final String httpResponseBody;
    private ValidationErrors validationErrors;

    public ApiException(String message, StatusLine statusLine, HttpResponse httpResponse,  String httpResponseBody) {
        super(message);
        this.statusLine = statusLine;
        this.httpResponse = httpResponse;
        this.httpResponseBody = httpResponseBody;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusLine = null;
        this.httpResponse = null;
        this.httpResponseBody = null;
    }

    public ApiException(String message) {
        super(message);
        this.statusLine = null;
        this.httpResponse = null;
        this.httpResponseBody = null;
    }

    public StatusLine getStatusLine() {
        return statusLine;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public ValidationErrors getValidationErrors() {
        return validationErrors;
    }

    public ApiException validationErrors(ValidationErrors validationErrors) {
        this.validationErrors = validationErrors;
        return this;
    }

    public String getHttpResponseBody() {
        return httpResponseBody;
    }

}
