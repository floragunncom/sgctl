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

package com.floragunn.searchguard.sgctl.commands.user;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.UnexpectedDocumentStructureException;
import com.floragunn.codova.documents.patch.MergePatch;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.ConditionalRequestHeader.IfMatch;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.client.api.GetUserResponse;
import com.floragunn.searchguard.sgctl.commands.ConnectingCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "update-user", description = "Updates a user")
public class UpdateUser extends ConnectingCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "1", description = "User name")
    private String userName;

    @Option(names = { "-r", "--sg-roles" }, split = ",")
    private List<String> sgRolesToAdd;

    @Option(names = { "--remove-sg-roles" }, split = ",")
    private List<String> sgRolesToRemove;

    @Option(names = { "--backend-roles" }, split = ",")
    private List<String> backendRolesToAdd;

    @Option(names = { "--remove-backend-roles" }, split = ",")
    private List<String> backendRolesToRemove;

    @Option(names = { "-a", "--attributes" }, split = ",")
    private Map<String, Object> attributesToAdd;

    @Option(names = { "--remove-attributes" }, split = ",")
    private List<String> attributesToRemove;

    @Option(names = { "--password" }, arity = "0..1", description = "Passphrase", interactive = true)
    String password;

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            retryOnConcurrencyConflict(() -> {

                GetUserResponse getUserResponse = client.getUser(userName);

                Map<String, Object> userMergePatch = new LinkedHashMap<>();

                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("Updating user ").append(userName);

                if (sgRolesToAdd != null || sgRolesToRemove != null) {
                    List<String> sgRoles = new ArrayList<>(getUserResponse.getSearchGuardRoles());

                    if (sgRolesToRemove != null) {
                        messageBuilder.append(" with SG roles to remove: ").append(String.join(",", sgRolesToRemove));
                        sgRoles.removeAll(sgRolesToRemove);
                    }

                    if (sgRolesToAdd != null) {
                        messageBuilder.append(" with SG roles: ").append(String.join(",", sgRolesToAdd));
                        sgRoles.addAll(sgRolesToAdd);
                    }

                    if (!sgRoles.equals(getUserResponse.getSearchGuardRoles())) {
                        userMergePatch.put("search_guard_roles", sgRoles);
                    }
                }

                if (backendRolesToAdd != null || backendRolesToRemove != null) {
                    List<String> backendRoles = new ArrayList<>(getUserResponse.getBackendRoles());

                    if (backendRolesToRemove != null) {
                        messageBuilder.append(" with backend roles to remove: ").append(String.join(",", backendRolesToRemove));
                        backendRoles.removeAll(backendRolesToRemove);
                    }
                    if (backendRolesToAdd != null) {
                        messageBuilder.append(" with backend roles: ").append(String.join(",", backendRolesToAdd));
                        backendRoles.addAll(backendRolesToAdd);
                    }

                    if (!backendRoles.equals(getUserResponse.getBackendRoles())) {
                        userMergePatch.put("backend_roles", backendRoles);
                    }
                }

                if (attributesToAdd != null || attributesToRemove != null) {

                    Map<String, Object> attributesPatch = new LinkedHashMap<>();

                    if (attributesToRemove != null) {
                        messageBuilder.append(" with attributes to remove: ").append(String.join(",", attributesToRemove));

                        for (String attribute : attributesToRemove) {
                            attributesPatch.put(attribute, null);
                        }
                    }

                    if (attributesToAdd != null) {
                        messageBuilder.append(" with attributes: ").append(attributesToAdd.entrySet().stream()
                                .map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));

                        for (Map.Entry<String, Object> entry : attributesToAdd.entrySet()) {
                            attributesPatch.put(entry.getKey(), entry.getValue());
                        }
                    }

                    userMergePatch.put("attributes", DocNode.wrap(attributesPatch).splitDottedAttributeNamesToTree().toMap());
                }

                if (password != null) {
                    userMergePatch.put("password", password);
                    messageBuilder.append(" with a new password");
                }

                if (verbose || debug) {
                    System.out.println(messageBuilder);
                }

                BasicResponse basicResponse = client.patchUser(userName, new MergePatch(DocNode.wrap(userMergePatch)),
                        new IfMatch(getUserResponse.getETag()));
                System.out.println(basicResponse.getMessage());
            });

            return 0;
        } catch (SgctlException | InvalidResponseException | FailedConnectionException | ServiceUnavailableException | UnauthorizedException
                | UnexpectedDocumentStructureException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (ApiException e) {
            if (e.getValidationErrors() != null) {
                System.err.println(e.getValidationErrors());
            } else {
                System.err.println(e.getMessage());
            }
            return 1;
        }
    }
}