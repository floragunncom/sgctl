package com.floragunn.searchguard.sgctl.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.floragunn.searchguard.sgctl.client.api.GetUserResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floragunn.codova.config.net.TLSConfig;
import com.floragunn.codova.documents.patch.DocPatch;
import org.apache.http.HttpHost;

@ExtendWith(MockitoExtension.class)
public class SearchGuardRestClientTest {

    public static final String STATUS_LINE_ERROR_PHRASE = "TEST ERROR RESPONSE STATUS LINE";
    @Mock
    private TLSConfig tlsConfig;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private DocPatch docPatch;

    private HttpHost httpHost;

    private SearchGuardRestClient restClient;

    @BeforeEach
    public void setup() throws Exception {
        httpHost = new HttpHost("localhost", 9200, "http");
        restClient = new SearchGuardRestClient(httpHost, tlsConfig, httpClient);
    }

    private void prepareHttpResponse(int statusCode, String body, String contentType, String eTag) throws Exception {
        BasicHttpEntity entity = new BasicHttpEntity();
        if (body != null) {
            entity.setContent(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        }
        if (contentType != null) {
            entity.setContentType(contentType);
        }
        when(httpResponse.getEntity()).thenReturn(entity);
        StatusLine sl = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, statusCode == 200 ? "OK" : STATUS_LINE_ERROR_PHRASE);
        when(httpResponse.getStatusLine()).thenReturn(sl);
        if (eTag != null) {
            when(httpResponse.containsHeader("ETag")).thenReturn(true);
            when(httpResponse.getFirstHeader("ETag")).thenReturn(new BasicHeader("ETag", eTag));
        } else {
            when(httpResponse.containsHeader("ETag")).thenReturn(false);
        }
        when(httpClient.execute(eq(httpHost), Mockito.any(HttpRequest.class))).thenReturn(httpResponse);
    }

    @Test
    public void shouldReturnGetUserResponseWhenUserExists() throws Exception {
        String json = "{\"data\":{\"description\":\"desc\",\"search_guard_roles\":[\"role1\"],\"backend_roles\":[\"b1\",\"b2\"],\"attributes\":{\"k\":\"v\"}}}";
        prepareHttpResponse(200, json, "application/json; charset=UTF-8", "etag-123");

        GetUserResponse resp = restClient.getUser("john doe+");

        assertThat(resp, notNullValue());
        assertThat(resp.getDescription(), equalTo("desc"));
        assertThat(resp.getSearchGuardRoles(), hasSize(1));
        assertThat(resp.getSearchGuardRoles().get(0), equalTo("role1"));
        assertThat(resp.getBackendRoles(), hasSize(2));
        assertThat(resp.getAttributes().get("k"), equalTo((Object) "v"));
        assertThat(resp.getETag(), equalTo("etag-123"));

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).execute(eq(httpHost), captor.capture());
        HttpRequest sentHttpRequest = captor.getValue();
        assertThat(sentHttpRequest, instanceOf(HttpUriRequest.class));
        String uri = sentHttpRequest.getRequestLine().getUri();
        assertThat(uri, containsString("/_searchguard/internal_users/john%20doe%2B"));
    }

    @Test
    public void shouldThrowApiExceptionWhenGetUserNotFound() throws Exception {
        prepareHttpResponse(404, "{}", "application/json; charset=UTF-8", null);

        ApiException ex = assertThrows(ApiException.class, () -> {
            restClient.getUser("nonexistent");
        });
        assertThat(ex.getMessage(), containsString(STATUS_LINE_ERROR_PHRASE));
    }

    @Test
    public void shouldThrowFailedConnectionWhenHttpClientFailsOnGetUser() throws Exception {
        when(httpClient.execute(eq(httpHost), Mockito.any(HttpRequest.class))).thenThrow(new java.io.IOException("Test IO Error"));

        FailedConnectionException ex = assertThrows(FailedConnectionException.class, () -> {
            restClient.getUser("any");
        });
        assertThat(ex.getMessage(), containsString("Test IO Error"));
    }

    @Test
    public void shouldReturnBasicResponseWhenDeleteUserSucceeds() throws Exception {
        String responseBody = "{\"message\":\"deleted\"}";
        prepareHttpResponse(200, responseBody, "application/json; charset=UTF-8", null);

        BasicResponse resp = restClient.deleteUser("alice/bob");

        assertThat(resp, notNullValue());
        assertThat(resp.getMessage(), equalTo("deleted"));

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).execute(eq(httpHost), captor.capture());
        HttpRequest sentHttpRequest = captor.getValue();
        assertThat(sentHttpRequest, instanceOf(HttpUriRequest.class));
        String uri = sentHttpRequest.getRequestLine().getUri();
        assertThat(uri, containsString("/_searchguard/internal_users/alice%2Fbob"));
    }

    @Test
    public void shouldSendPutWithBodyAndReturnBasicResponse() throws Exception {
        String respJson = "{\"message\":\"ok\"}";
        prepareHttpResponse(200, respJson, "application/json; charset=UTF-8", null);

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("password", "secret");
        newUser.put("description", "d");

        BasicResponse response = restClient.putUser("bob@with? space #and[reserved]characters/!", newUser);

        assertThat(response, notNullValue());
        assertThat(response.getMessage(), equalTo("ok"));

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).execute(eq(httpHost), captor.capture());
        HttpRequest sentHttpRequest = captor.getValue();
        assertThat(sentHttpRequest, instanceOf(org.apache.http.HttpEntityEnclosingRequest.class));
        HttpEntity entity = ((org.apache.http.HttpEntityEnclosingRequest) sentHttpRequest).getEntity();
        String sentBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        assertThat(sentBody, containsString("\"password\""));
        assertThat(sentBody, containsString("\"secret\""));

        String uri = sentHttpRequest.getRequestLine().getUri();
        assertThat(uri, containsString("/_searchguard/internal_users/bob%40with%3F%20space%20%23and%5Breserved%5Dcharacters%2F%21"));
    }

    @Test
    public void shouldThrowApiExceptionWhenPutUserBadRequestWithJsonError() throws Exception {
        String errorJson = "{\"error\":\"invalid data\"}";
        prepareHttpResponse(400, errorJson, "application/json; charset=UTF-8", null);

        Map<String, Object> newUser = Collections.singletonMap("x", "y");

        ApiException ex = assertThrows(ApiException.class, () -> {
            restClient.putUser("bob", newUser);
        });
        assertThat(ex.getMessage(), containsString("invalid data"));
    }

    @Test
    public void shouldSendPatchWithDocPatchBodyAndHeaders() throws Exception {
        String respJson = "{\"message\":\"patched\"}";
        prepareHttpResponse(200, respJson, "application/json; charset=UTF-8", null);

        when(docPatch.toJsonString()).thenReturn("{\"op\":\"replace\"}");
        when(docPatch.getMediaType()).thenReturn("application/json-patch+json");

        Header h = new BasicHeader("X-Custom", "v1");

        BasicResponse resp = restClient.patchUser("u1", docPatch, h);

        assertThat(resp, notNullValue());
        assertThat(resp.getMessage(), equalTo("patched"));

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).execute(eq(httpHost), captor.capture());
        HttpRequest sentHttpRequest = captor.getValue();

        assertThat(sentHttpRequest, instanceOf(org.apache.http.HttpEntityEnclosingRequest.class));
        HttpEntity entity = ((org.apache.http.HttpEntityEnclosingRequest) sentHttpRequest).getEntity();
        String sentBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        assertThat(sentBody, containsString("\"op\":\"replace\""));

        Header[] headers = sentHttpRequest.getAllHeaders();
        boolean found = false;
        for (Header hh : headers) {
            if ("X-Custom".equals(hh.getName()) && "v1".equals(hh.getValue())) {
                found = true;
                break;
            }
        }
        assertThat(found, is(true));

        Header contentTypeHeader = entity.getContentType();
        assertThat(contentTypeHeader, notNullValue());
        assertThat(contentTypeHeader.getValue(), containsString("application/json-patch+json"));
    }
}
