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
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuthTokenResponse {
    private final AsciiTable table = new AsciiTable();
    private final Gson g = new Gson();
    private final Map<String, Object> content;

    public AuthTokenResponse(DocNode content)  {
        System.out.println("TTTTT" + content.get("hits"));
        this.content = content;
    }

    public AuthTokenResponse(SearchGuardRestClient.Response response) throws InvalidResponseException {
        this(response.asDocNode());
    }

    public List<AuthTokenDTO> parseAuthTokenEntries(ArrayList<JsonElement> entries) {
        List<AuthTokenDTO> authTokenDTOS = new ArrayList<>();


        for (JsonElement entry : entries) {
            AuthTokenDTO token = g.fromJson(entry.getAsJsonObject().get("_source"), AuthTokenDTO.class);
            authTokenDTOS.add(token);
        }
        return authTokenDTOS;
    }

    public ArrayList<JsonElement> getEntries() {

        ArrayList<JsonElement> entriesList = new ArrayList<>();

        String hits = content.get("hits").toString();
        List<JsonElement> entriesFromResponse = JsonParser.parseString(hits).getAsJsonObject().get("hits").getAsJsonArray().asList();
        entriesList.addAll(entriesFromResponse);

        return entriesList;
    }

    protected String getAsString(String name) {
        return content.get(name) != null ? String.valueOf(content.get(name)) : null;
    }

    public String toString() {

        ArrayList<JsonElement> entries  = getEntries();
        if(entries.size() == 0) {
            return "No AuthTokens found";
        }
      return entries.size() + " entries returned and listed below \n" + buildAsciiTableOutput();
    }

    private String  buildAsciiTableOutput() {
        table.addRule();
        table.addRow("Username", "Token name", "Created at", "Expires at", "Revoked at", "Cluster permissions");
        table.addRule();
        table.getContext().setWidth(200);

        for (AuthTokenDTO token : parseAuthTokenEntries(getEntries())) {
            // AsciiTable library used removes double spaces before rendering, so replacing the double spaces for ±±,  a bit hacky solution to keep the yaml formatting.
            table.addRow(token.getUser_name(), token.getToken_name(), token.getCreated_at(), token.getExpires_at(), token.getRevoked_at(),
                    token.getRequested().toYaml().replace("\n", "<br/>").replace("  ", "±±"));
            table.addRule();
        }
        table.setPaddingLeft(1);
        table.setTextAlignment(TextAlignment.LEFT);
        return table.render().replace("±±", "  ");
    }

}
