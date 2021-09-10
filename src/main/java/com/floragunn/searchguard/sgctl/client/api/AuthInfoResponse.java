package com.floragunn.searchguard.sgctl.client.api;

import java.util.Map;

import com.floragunn.codova.documents.DocNode;

public class AuthInfoResponse {

    private final Map<String, Object> content;

    public AuthInfoResponse(Map<String, Object> content) {
        this.content = content;
    }
    
    public AuthInfoResponse(DocNode content) {
        this.content = content;
    }

    public String getUserName() {
        return getAsString("user_name");
    }

    protected String getAsString(String name) {
        return content.get(name) != null ? String.valueOf(content.get(name)) : null;
    }

}
