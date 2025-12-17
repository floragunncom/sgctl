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

/**
 * Wraps HTTP-level errors returned by the Search Guard REST API.
 */
public class ApiException extends Exception {

    private static final long serialVersionUID = -2613151852034285098L;
    private final StatusLine statusLine;
    private final HttpResponse httpResponse;
    private final String httpResponseBody;
    private ValidationErrors validationErrors;

    /**
     * Creates an exception containing the raw HTTP status and payload.
     *
     * @param message human-readable error message
     * @param statusLine HTTP status line returned by the server
     * @param httpResponse raw HTTP response object
     * @param httpResponseBody response body content
     */
    public ApiException(String message, StatusLine statusLine, HttpResponse httpResponse,  String httpResponseBody) {
        super(message);
        this.statusLine = statusLine;
        this.httpResponse = httpResponse;
        this.httpResponseBody = httpResponseBody;
    }

    /**
     * Creates an exception with a cause.
     *
     * @param message human-readable error message
     * @param cause underlying cause
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusLine = null;
        this.httpResponse = null;
        this.httpResponseBody = null;
    }

    /**
     * Creates an exception with only a message.
     *
     * @param message human-readable error message
     */
    public ApiException(String message) {
        super(message);
        this.statusLine = null;
        this.httpResponse = null;
        this.httpResponseBody = null;
    }

    /**
     * Returns the HTTP status line if provided.
     *
     * @return HTTP status line, if available
     */
    public StatusLine getStatusLine() {
        return statusLine;
    }

    /**
     * Returns the raw HTTP response object if provided.
     *
     * @return raw HTTP response, if available
     */
    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    /**
     * Returns validation errors returned by the server, if any.
     *
     * @return validation errors or {@code null}
     */
    public ValidationErrors getValidationErrors() {
        return validationErrors;
    }

    /**
     * Attaches validation errors to this exception.
     *
     * @param validationErrors validation errors to attach
     * @return this instance for chaining
     */
    public ApiException validationErrors(ValidationErrors validationErrors) {
        this.validationErrors = validationErrors;
        return this;
    }

    /**
     * Returns the response body as text.
     *
     * @return response body, if available
     */
    public String getHttpResponseBody() {
        return httpResponseBody;
    }

}
