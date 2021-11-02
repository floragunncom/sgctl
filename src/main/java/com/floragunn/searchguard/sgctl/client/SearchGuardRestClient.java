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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
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
import com.floragunn.codova.documents.DocParseException;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocType;
import com.floragunn.codova.documents.DocType.UnknownDocTypeException;
import com.floragunn.codova.documents.DocUtils;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.UnexpectedDocumentStructureException;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingFunction;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.searchguard.sgctl.client.api.AuthInfoResponse;
import com.floragunn.searchguard.sgctl.client.api.GetBulkConfigResponse;
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
        return get("/_searchguard/authinfo").by(AuthInfoResponse::new);
    }

    public BasicResponse putConfigBulk(Map<String, Map<String, Map<String, Object>>> configTypeToConfigMap)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return putJson("/_searchguard/config", configTypeToConfigMap).by(BasicResponse::new);
    }

    public GetBulkConfigResponse getConfigBulk()
            throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException, FailedConnectionException {
        return get("/_searchguard/config").by(GetBulkConfigResponse::new);
    }

    public BasicResponse getUser(String userName)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return get("/_searchguard/internal_users/" + userName).by(BasicResponse::new);
    }

    public BasicResponse deleteUser(String userName)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return delete("/_searchguard/internal_users/" + userName).by(BasicResponse::new);
    }

    public BasicResponse putUser(String userName, Map<String, Object> newUserData)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return putJson("/_searchguard/internal_users/" + userName, newUserData).by(BasicResponse::new);
    }

    public BasicResponse patchUser(String userName, Map<String, Object> userUpdateData)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return patchJson("/_searchguard/internal_users/" + userName, userUpdateData).by(BasicResponse::new);
    }

    public BasicResponse putSgConfig(Map<String, Object> body)
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return putJson("/_searchguard/api/sg_config", body).by(BasicResponse::new);
    }

    public BasicResponse reloadHttpCerts()
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return post("/_searchguard/api/ssl/http/reloadcerts/").by(BasicResponse::new);
    }

    public BasicResponse reloadTransportCerts()
            throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return post("/_searchguard/api/ssl/transport/reloadcerts/").by(BasicResponse::new);
    }

    public BasicResponse getComponentState(String componentId, boolean verbose)
            throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException, FailedConnectionException {
        return get("/_searchguard/component/" + (componentId != null ? componentId : "_all") + "/_health?verbose=" + verbose).by(BasicResponse::new);
    }

    protected Response get(String path) throws FailedConnectionException, InvalidResponseException {
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

    protected Response post(String path, String body, ContentType contentType) throws FailedConnectionException, InvalidResponseException {
        try {
            HttpPost httpPost = new HttpPost(path);
            httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
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

    protected Response post(String path) throws FailedConnectionException, InvalidResponseException {
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

    protected Response put(String path, String body, ContentType contentType) throws FailedConnectionException, InvalidResponseException {
        try {
            HttpPut httpPut = new HttpPut(path);
            httpPut.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
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

    protected Response putJson(String path, Map<String, ?> body) throws FailedConnectionException, InvalidResponseException {
        return put(path, DocWriter.json().writeAsString(body), ContentType.APPLICATION_JSON);
    }

    protected Response patchJson(String path, Map<String, ?> body) throws FailedConnectionException, InvalidResponseException {
        return patch(path, DocWriter.json().writeAsString(body), ContentType.APPLICATION_JSON);
    }

    protected Response patch(String path, String body, ContentType contentType) throws FailedConnectionException, InvalidResponseException {
        try {
            HttpPatch httpPatch = new HttpPatch(path);
            httpPatch.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
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

    protected Response delete(String path) throws FailedConnectionException, InvalidResponseException {
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

        Response(HttpResponse httpResponse) throws InvalidResponseException {
            this.httpResponse = httpResponse;
            this.contentType = getContentType(httpResponse);

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

        Map<String, Object> asMap() throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException {
            checkStatus();
            try {
                return DocReader.type(DocType.getByContentType(contentType)).readObject(bodyAsString);
            } catch (UnknownDocTypeException | UnexpectedDocumentStructureException | DocParseException e) {
                throw new InvalidResponseException(e);
            }
        }

        DocNode asDocNode() throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException {
            checkStatus();
            if(bodyAsString == null) {
                return DocNode.EMPTY;
            }
            try {
                DocType docType = DocType.peekByContentType(contentType);

                if (docType != null) {
                    return DocNode.wrap(DocReader.type(docType).read(bodyAsString));
                } else {
                    return DocNode.wrap(bodyAsString);
                }
            } catch (DocParseException e) {
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

        public <T> T by(ValidatingFunction<DocNode, T> parser)
                throws InvalidResponseException, ServiceUnavailableException, UnauthorizedException, ApiException {

            checkStatus();

            try {
                return parser.apply(asDocNode());
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
                    throw parseBadRequestJsonRespose();
                } else {
                    String message = getStatusMessage("Bad Request");

                    throw new ApiException(message, httpResponse.getStatusLine(), httpResponse);
                }
            } else if (statusCode == 404) {
                    String message = getStatusMessage("Not found");
                    throw new ApiException(message, httpResponse.getStatusLine(), httpResponse);
            } else if (statusCode > 400) {
                String message = getStatusMessage("Bad Request");

                throw new ApiException(message, httpResponse.getStatusLine(), httpResponse);
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
                        }

                        if(error instanceof Map) {
                            if(((Map) error).containsKey("message")); {
                                Object errorMessage = ((Map<?, ?>) error).get("message");
                                if(errorMessage instanceof String) {
                                    return (String) errorMessage;
                                }
                            }
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

        private ApiException parseBadRequestJsonRespose() throws InvalidResponseException {
            try {
                DocNode response = DocNode.wrap(DocReader.json().read(this.bodyAsString));
                String errorMessage = null;
                ValidationErrors validationErrors = null;

                if (response.get("error") instanceof String) {
                    errorMessage = (String) response.get("error");
                }

                if (response.get("detail") instanceof Map) {
                    try {
                        validationErrors = ValidationErrors.parse(DocUtils.toStringKeyedMap((Map<?, ?>) response.get("detail")));
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Error while parsing validation errors in response", e);
                    }
                }

                return new ApiException(errorMessage, httpResponse.getStatusLine(), httpResponse).validationErrors(validationErrors);

            } catch (DocParseException e) {
                throw new InvalidResponseException("Response contains invalid JSON: " + e.getMessage(), e);
            }

        }

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

    private static String getContentType(HttpResponse response) {
        if(response.getEntity() == null) {
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
