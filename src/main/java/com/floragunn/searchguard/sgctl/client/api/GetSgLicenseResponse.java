package com.floragunn.searchguard.sgctl.client.api;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;

public class GetSgLicenseResponse {
    private final DocNode content;
    private final DocNode sgLicense;

    public GetSgLicenseResponse(DocNode node) {
        content = node;
        sgLicense = content.hasNonNull("sg_license") ? content.getAsNode("sg_license") : DocNode.EMPTY;
    }

    public GetSgLicenseResponse(SearchGuardRestClient.Response response) throws InvalidResponseException {
        this(response.asDocNode());
    }

    public String getExpiryString() {
        return sgLicense.hasNonNull("expiry_date") ? sgLicense.getAsString("expiry_date") : null;
    }
}
