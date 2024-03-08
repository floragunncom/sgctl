/*
 * Copyright 2021-2022 floragunn GmbH
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;

import com.floragunn.searchguard.sgctl.client.api.GetSgLicenseResponse;
import com.floragunn.searchguard.sgctl.client.api.AuthTokenResponse;
import com.floragunn.searchguard.sgctl.client.api.RevokeAuthTokenResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.floragunn.codova.config.net.TLSConfig;
import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocUtils;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Format;
import com.floragunn.codova.documents.Format.UnknownDocTypeException;
import com.floragunn.codova.documents.UnexpectedDocumentStructureException;
import com.floragunn.codova.documents.patch.DocPatch;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingFunction;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.searchguard.sgctl.client.api.AuthInfoResponse;
import com.floragunn.searchguard.sgctl.client.api.GetBulkConfigResponse;
import com.floragunn.searchguard.sgctl.client.api.GetUserResponse;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public class SearchGuardRestClient implements AutoCloseable {
    private static final Logger log = Logger.getLogger(SearchGuardRestClient.class.getName());

    private final HttpHost httpHost;
    private final TLSConfig tlsConfig;
    private CloseableHttpClient client;
    private boolean debug;

    public SearchGuardRestClient(HttpHost httpHost, TLSConfig tlsConfig) {
        this.httpHost = httpHost;
        this.tlsConfig = tlsConfig;
        this.client = HttpClientBuilder.create().setSSLSocketFactory(tlsConfig.toSSLConnectionSocketFactory()).build();
    }

    public AuthInfoResponse authInfo()
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return get("/_searchguard/authinfo").parseResponseBy(AuthInfoResponse::new);
    }

    public AuthTokenResponse listAuthTokens()  throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException, FailedConnectionException {
        return get("/_searchguard/authtoken/_search")
                .parseResponseBy(AuthTokenResponse::new);

    }

    public RevokeAuthTokenResponse revokeAuthToken(String id)  throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException, FailedConnectionException {
        return delete("/_searchguard/authtoken/"+  id)
                .parseResponseBy(RevokeAuthTokenResponse::new);

    }

    public BasicResponse putConfigBulk(Map<String, Map<String, ?>> configTypeToConfigMap)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return putJson("/_searchguard/config", configTypeToConfigMap).parseResponseBy(BasicResponse::new);
    }

    public GetBulkConfigResponse getConfigBulk()
            throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException, FailedConnectionException {
        return get("/_searchguard/config").parseResponseBy(GetBulkConfigResponse::new);
    }

    public GetUserResponse getUser(String userName)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return get("/_searchguard/internal_users/" + userName).parseResponseBy(GetUserResponse::new);
    }

    public BasicResponse deleteUser(String userName)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return delete("/_searchguard/internal_users/" + userName).parseResponseBy(BasicResponse::new);
    }

    public BasicResponse putUser(String userName, Map<String, Object> newUserData)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return putJson("/_searchguard/internal_users/" + userName, newUserData).parseResponseBy(BasicResponse::new);
    }

    public BasicResponse patchUser(String userName, DocPatch patch, Header... headers)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return patch("/_searchguard/internal_users/" + userName, patch, headers).parseResponseBy(BasicResponse::new);
    }

    public BasicResponse putConfigVar(String id, Object value, String scope, boolean encrypt, Header... headers)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("value", value);

        if (scope != null) {
            doc.put("scope", scope);
        }

        if (encrypt) {
            doc.put("encrypt", true);
        }

        return putJson("/_searchguard/config/vars/" + id, doc, headers).parseResponseBy(BasicResponse::new);
    }

    public BasicResponse deleteConfigVar(String id)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return delete("/_searchguard/config/vars/" + id).parseResponseBy(BasicResponse::new);
    }

    public BasicResponse getAllConfigVars()
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return delete("/_searchguard/config/vars").parseResponseBy(BasicResponse::new);
    }

    public BasicResponse putSgConfig(Map<String, Object> body)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return putJson("/_searchguard/api/sg_config", body).parseResponseBy(BasicResponse::new);
    }

    public BasicResponse reloadHttpCerts()
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return post("/_searchguard/api/ssl/http/reloadcerts/").parseResponseBy(BasicResponse::new);
    }

    public BasicResponse reloadTransportCerts()
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return post("/_searchguard/api/ssl/transport/reloadcerts/").parseResponseBy(BasicResponse::new);
    }

    public BasicResponse getComponentState(String componentId, boolean verbose)
            throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException, FailedConnectionException {
        return get("/_searchguard/component/" + (componentId != null ? componentId : "_all") + "/_health?verbose=" + verbose)
                .parseResponseBy(BasicResponse::new);
    }

    public GetSgLicenseResponse getSgLicense()
            throws FailedConnectionException, InvalidResponseException, UnauthorizedException, ServiceUnavailableException, ApiException {
        return get("/_searchguard/license").parseResponseBy(GetSgLicenseResponse::new);
    }

    public BasicResponse putSgLicense(Map<String, Object> body)
            throws FailedConnectionException, InvalidResponseException, UnauthorizedException, ServiceUnavailableException, ApiException {
        return putJson("/_searchguard/license/key", body).parseResponseBy(BasicResponse::new);
    }

    public Response get(String path) throws FailedConnectionException, InvalidResponseException {
        try {
            return new Response(client.execute(httpHost, new HttpGet(path)));
        } catch (ClientProtocolException e) {
            throw new FailedConnectionException(e);
        } catch (ConnectException e) {
            throw new FailedConnectionException(e.getMessage(), e);
        } catch (SSLHandshakeException e) {
            throw new FailedConnectionException("TLS handshake failed while creating new connection: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new FailedConnectionException(e);
        }
    }

    public Response post(String path, String body, ContentType contentType) throws FailedConnectionException, InvalidResponseException {
        try {
            HttpPost httpPost = new HttpPost(path);
            httpPost.setEntity(new StringEntity(body, contentType));
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, contentType.getMimeType());
            return new Response(client.execute(httpHost, httpPost));
        } catch (ClientProtocolException e) {
            throw new FailedConnectionException(e);
        } catch (ConnectException e) {
            throw new FailedConnectionException(e.getMessage(), e);
        } catch (SSLHandshakeException e) {
            throw new FailedConnectionException("TLS handshake failed while creating new connection: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new FailedConnectionException(e);
        }
    }

    protected Response postJson(String path, Map<String, Object> body) throws FailedConnectionException, InvalidResponseException {
        return post(path, DocWriter.json().writeAsString(body), ContentType.APPLICATION_JSON);
    }

    public Response post(String path) throws FailedConnectionException, InvalidResponseException {
        try {
            HttpPost httpPost = new HttpPost(path);
            return new Response(client.execute(httpHost, httpPost));
        } catch (ClientProtocolException e) {
            throw new FailedConnectionException(e);
        } catch (ConnectException e) {
            throw new FailedConnectionException(e.getMessage(), e);
        } catch (SSLHandshakeException e) {
            throw new FailedConnectionException("TLS handshake failed while creating new connection: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new FailedConnectionException(e);
        }
    }

    public Response put(String path, String body, ContentType contentType, Header... headers)
            throws FailedConnectionException, InvalidResponseException {
        try {
            HttpPut httpPut = new HttpPut(path);

            if (headers != null) {
                httpPut.setHeaders(headers);
            }

            httpPut.setEntity(new StringEntity(body, contentType));

            return new Response(client.execute(httpHost, httpPut));
        } catch (ClientProtocolException e) {
            throw new FailedConnectionException(e);
        } catch (ConnectException e) {
            throw new FailedConnectionException(e.getMessage(), e);
        } catch (SSLHandshakeException e) {
            throw new FailedConnectionException("TLS handshake failed while creating new connection: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new FailedConnectionException(e);
        }
    }

    protected Response putJson(String path, Map<String, ?> body, Header... headers) throws FailedConnectionException, InvalidResponseException {
        return put(path, DocWriter.json().writeAsString(body), ContentType.APPLICATION_JSON, headers);
    }

    public Response patch(String path, DocPatch patch, Header... headers) throws FailedConnectionException, InvalidResponseException {
        return patch(path, patch.toJsonString(), ContentType.create(patch.getMediaType()), headers);
    }

    public Response patch(String path, String body, ContentType contentType, Header... headers)
            throws FailedConnectionException, InvalidResponseException {
        try {
            HttpPatch httpPatch = new HttpPatch(path);

            if (headers != null) {
                httpPatch.setHeaders(headers);
            }

            httpPatch.setEntity(new StringEntity(body, contentType));

            return new Response(client.execute(httpHost, httpPatch));
        } catch (ClientProtocolException e) {
            throw new FailedConnectionException(e);
        } catch (ConnectException e) {
            throw new FailedConnectionException(e.getMessage(), e);
        } catch (SSLHandshakeException e) {
            throw new FailedConnectionException("TLS handshake failed while creating new connection: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new FailedConnectionException(e);
        }
    }

    public Response delete(String path) throws FailedConnectionException, InvalidResponseException {
        try {
            return new Response(client.execute(httpHost, new HttpDelete(path)));
        } catch (ClientProtocolException e) {
            throw new FailedConnectionException(e);
        } catch (ConnectException e) {
            throw new FailedConnectionException(e.getMessage(), e);
        } catch (SSLHandshakeException e) {
            throw new FailedConnectionException("TLS handshake failed while creating new connection: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new FailedConnectionException(e);
        }
    }

    public HttpHost getHttpHost() {
        return httpHost;
    }

    public TLSConfig getTlsConfig() {
        return tlsConfig;
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class Response {
        private final HttpResponse httpResponse;
        private final String bodyAsString;
        private final String contentType;
        private final String eTag;
        private final String searchGuardVersion;

        Response(HttpResponse httpResponse) throws InvalidResponseException {
            this.httpResponse = httpResponse;
            this.contentType = getContentTypeFromResponse(httpResponse);
            this.eTag = httpResponse.containsHeader("ETag") ? httpResponse.getFirstHeader("ETag").getValue() : null;
            this.searchGuardVersion = httpResponse.containsHeader("X-Search-Guard-Version")
                    ? httpResponse.getFirstHeader("X-Search-Guard-Version").getValue()
                    : null;

            if (debug) {
                System.out.println("------------------------------------------------");
                System.out.println(this.httpResponse.getStatusLine());
                System.out.println("Content-Type: " + this.contentType);
            }

            try {
                this.bodyAsString = getEntityAsString(httpResponse);

                if (debug && this.bodyAsString != null && this.bodyAsString.length() > 0) {
                    System.out.println(abbreviate(bodyAsString, 240));
                }

                if (debug) {
                    System.out.println("------------------------------------------------");
                }
            } catch (IllegalCharsetNameException | UnsupportedCharsetException | IOException e) {
                throw new InvalidResponseException(e);
            }
        }

        private String abbreviate(String string, int length) {
            if (string.length() <= length) {
                return string;
            } else {
                return string.substring(0, length) + "...";
            }
        }

        Map<String, Object> asMap()
                throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException, PreconditionFailedException {
            checkStatus();
            try {
                return DocReader.format(Format.getByContentType(contentType)).readObject(bodyAsString);
            } catch (UnknownDocTypeException | UnexpectedDocumentStructureException | DocumentParseException e) {
                throw new InvalidResponseException(e);
            }
        }

        public DocNode asDocNode() throws InvalidResponseException {
            if (bodyAsString == null) {
                return DocNode.EMPTY;
            }
            try {
                Format docType = Format.peekByContentType(contentType);

                if (docType != null) {
                    return DocNode.wrap(DocReader.format(docType).read(bodyAsString));
                } else {
                    return DocNode.wrap(bodyAsString);
                }
            } catch (DocumentParseException e) {
                throw new InvalidResponseException(e);
            }
        }

        public <T> T byString(ValidatingFunction<String, T> parser)
                throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException {

            checkStatus();

            try {
                return parser.apply(bodyAsString);
            } catch (ConfigValidationException e) {
                throw new InvalidResponseException(e);
            } catch (Exception e) {
                throw new InvalidResponseException(e);
            }
        }

        public <T> T parseResponseBy(ResponseParser<T> parser)
                throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException {

            checkStatus();

            try {
                return parser.apply(this);
            } catch (ConfigValidationException e) {
                throw new InvalidResponseException(e);
            } catch (Exception e) {
                throw new InvalidResponseException(e);
            }
        }

        public HttpResponse getHttpResponse() {
            return httpResponse;
        }

        private void checkStatus() throws ServiceUnavailableException, UnauthorizedException, ApiException, InvalidResponseException {
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode == 500) {
                String message = getStatusMessage("Service unavailable: Internal server error");

                throw new ServiceUnavailableException(message, httpResponse.getStatusLine(), httpResponse);
            } else if (statusCode == 503) {
                String message = getStatusMessage("Service temporarily unavailable; please try again later");

                throw new ServiceUnavailableException(message, httpResponse.getStatusLine(), httpResponse);
            } else if (statusCode >= 500) {
                String message = getStatusMessage("Service unavailable: Error " + httpResponse.getStatusLine().getStatusCode());

                throw new ServiceUnavailableException(message, httpResponse.getStatusLine(), httpResponse);
            } else if (statusCode == 401) {
                String message = getStatusMessage("Unauthorized");

                throw new UnauthorizedException(message, httpResponse.getStatusLine(), httpResponse);
            } else if (statusCode == 400) {
                if ("application/json".equals(contentType)) {
                    throw parseBadRequestJsonResponse();
                } else {
                    String message = getStatusMessage("Bad Request");

                    throw new ApiException(message, httpResponse.getStatusLine(), httpResponse, bodyAsString);
                }
            } else if (statusCode == 404) {
                String message = getStatusMessage("Not found");
                throw new ApiException(message, httpResponse.getStatusLine(), httpResponse, bodyAsString);
            } else if (statusCode == 412) {
                String message = getStatusMessage("Precondition failed");
                throw new PreconditionFailedException(message, httpResponse.getStatusLine(), httpResponse, bodyAsString);
            } else if (statusCode > 400) {
                String message = getStatusMessage("Bad Request");

                throw new ApiException(message, httpResponse.getStatusLine(), httpResponse, bodyAsString);
            }

        }

        private String getStatusMessage(String fallback) {
            if ("application/json".equals(contentType)) {
                try {
                    Object document = DocReader.json().read(this.bodyAsString);

                    if (document instanceof Map) {
                        Object error = ((Map<?, ?>) document).get("error");

                        if (error instanceof String) {
                            return (String) error;
                        } else if (error instanceof Map) {
                            if (((Map<?, ?>) error).get("message") instanceof String) {
                                return (String) ((Map<?, ?>) error).get("message");
                            }
                        }

                        Object message = ((Map<?, ?>) document).get("message");

                        if (message != null) {
                            return message.toString();
                        }
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Error while parsing JSON response", e);
                }
            }

            String message = httpResponse.getStatusLine().getReasonPhrase();

            if (message != null && message.length() != 0) {
                return message;
            } else {
                return fallback;
            }
        }

        private ApiException parseBadRequestJsonResponse() throws InvalidResponseException {
            try {
                DocNode response = DocNode.wrap(DocReader.json().read(this.bodyAsString));
                String errorMessage = null;
                ValidationErrors validationErrors = null;

                if (response.get("error") instanceof String) {
                    errorMessage = (String) response.get("error");
                } else if (response.get("error") instanceof Map) {
                    DocNode errorNode = response.getAsNode("error");

                    if (errorNode.get("reason") instanceof String) {
                        errorMessage = (String) errorNode.get("reason");

                        if (errorMessage.startsWith("Invalid index name [_searchguard]")) {
                            errorMessage = "Invalid REST endpoint";
                        }
                    }

                    if (errorNode.get("message") instanceof String) {
                        errorMessage = (String) errorNode.get("message");
                    }

                    if (errorNode.get("details") instanceof Map) {
                        try {
                            validationErrors = ValidationErrors.parse(DocUtils.toStringKeyedMap((Map<?, ?>) errorNode.get("details")));
                        } catch (Exception e) {
                            log.log(Level.WARNING, "Error while parsing validation errors in response", e);
                        }
                    }
                }

                if (response.get("detail") instanceof Map) {
                    try {
                        validationErrors = ValidationErrors.parse(DocUtils.toStringKeyedMap((Map<?, ?>) response.get("detail")));
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Error while parsing validation errors in response", e);
                    }
                }

                if (errorMessage == null) {
                    errorMessage = this.httpResponse.getStatusLine().toString();
                }

                return new ApiException(errorMessage, this.httpResponse.getStatusLine(), this.httpResponse, this.bodyAsString)
                        .validationErrors(validationErrors);

            } catch (DocumentParseException e) {
                throw new InvalidResponseException("Response contains invalid JSON: " + e.getMessage(), e);
            }

        }

        public String getETag() {
            return eTag;
        }

        public String getSearchGuardVersion() {
            return searchGuardVersion;
        }

        public String getContentType() {
            return contentType;
        }

    }

    @FunctionalInterface
    public static interface ResponseParser<R> {
        R apply(Response response) throws InvalidResponseException, ConfigValidationException, ApiException;
    }

    private static String getEntityAsString(HttpResponse response) throws IllegalCharsetNameException, UnsupportedCharsetException, IOException {
        if (response == null || response.getEntity() == null) {
            return null;
        }

        return CharStreams.toString(new InputStreamReader(response.getEntity().getContent(), getContentEncoding(response.getEntity())));
    }

    private static Charset getContentEncoding(HttpEntity entity) throws IllegalCharsetNameException, UnsupportedCharsetException {
        return entity.getContentEncoding() != null ? Charset.forName(entity.getContentEncoding().getValue()) : Charsets.UTF_8;
    }

    private static String getContentTypeFromResponse(HttpResponse response) {
        if (response.getEntity() == null) {
            return null;
        }
        Header header = response.getEntity().getContentType();

        if (header == null) {
            return null;
        }

        ContentType contentType = ContentType.parse(header.getValue());

        return contentType.getMimeType().toLowerCase();
    }

    public boolean isDebug() {
        return debug;
    }

    public SearchGuardRestClient debug(boolean debug) {
        this.debug = debug;
        return this;
    }

}
