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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.commands.ConnectingCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "update-user", description = "Updates a user")
public class UpdateUser extends ConnectingCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "User name")
    private String userName;

    @Option(names = { "-r", "--sg-roles" }, split = ",")
    private List<String> sgRoles;

    @Option(names = { "--remove-sg-roles" }, split = ",")
    private List<String> sgRolesToRemove;

    @Option(names = { "--backend-roles" }, split = ",")
    private List<String> backendRoles;

    @Option(names = { "--remove-backend-roles" }, split = ",")
    private List<String> backendRolesToRemove;

    @Option(names = { "-a", "--attributes" }, split = ",")
    private Map<String, Object> attributes;

    @Option(names = { "--remove-attributes" }, split = ",")
    private List<String> attributesToRemove;

    @Option(names = { "--password" }, arity = "0..1", description = "Passphrase", interactive = true)
    String password;

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            Map<String, Object> userData = getUserData(client);
            List<String> previousSgRoles = (List<String>) userData.get("search_guard_roles");
            List<String> previousBackendRoles = (List<String>) userData.get("backend_roles");
            Map<String, Object> previousAttributes = (Map<String, Object>) userData.get("attributes");

            Map<String, Object> userUpdateData = new HashMap<>();

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Updating a user: ").append(userName);
            if (sgRoles != null || sgRolesToRemove != null) {
                if (sgRolesToRemove != null) {
                    messageBuilder.append(" with SG roles to remove: ").append(String.join(",", sgRolesToRemove));
                    previousSgRoles.removeAll(sgRolesToRemove);
                }
                if (sgRoles != null) {
                    messageBuilder.append(" with SG roles: ").append(String.join(",", sgRoles));
                    previousSgRoles.addAll(sgRoles);
                }
                userUpdateData.put("search_guard_roles", previousSgRoles);
            }

            if (backendRoles != null || backendRolesToRemove != null) {
                if (backendRolesToRemove != null) {
                    messageBuilder.append(" with backend roles to remove: ").append(String.join(",", backendRolesToRemove));
                    previousBackendRoles.removeAll(backendRolesToRemove);
                }
                if (backendRoles != null) {
                    messageBuilder.append(" with backend roles: ").append(String.join(",", backendRoles));
                    previousBackendRoles.addAll(backendRoles);
                }
                userUpdateData.put("backend_roles", previousBackendRoles);
            }

            if (attributes != null || attributesToRemove != null) {
                if (attributesToRemove != null) {
                    messageBuilder.append(" with attributes to remove: ").append(String.join(",", attributesToRemove));
                    removeAttributes(previousAttributes, attributesToRemove);
                }
                if (attributes != null) {
                    messageBuilder.append(" with attributes: ").append(
                            attributes.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
                    addAttributes(previousAttributes, attributes);
                }
                userUpdateData.put("attributes", previousAttributes);

            }

            if (password != null) {
                userUpdateData.put("password", password);
                messageBuilder.append(" with a new password");
            }

            if (verbose || debug) {
                System.out.println(messageBuilder);
            }

            BasicResponse basicResponse = client.patchUser(userName, userUpdateData);
            System.out.println(basicResponse.getMessage());

            return 0;
        } catch (SgctlException | InvalidResponseException | FailedConnectionException | ServiceUnavailableException | UnauthorizedException e) {
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

    private void addAttributes(Map<String, Object> userAttributes, Map<String, Object> attributesToAdd) {
        if (userAttributes.isEmpty()) {
            userAttributes.putAll(attributesToAdd);
            return;
        }
        for (Map.Entry<String, Object> entry : attributesToAdd.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                if (userAttributes.containsKey(key)) {
                    Object userAttributeUnderKey = userAttributes.get(key);
                    if (userAttributeUnderKey instanceof Map) {
                        addAttributes((Map<String, Object>) userAttributeUnderKey, (Map<String, Object>) value);
                    } else {
                        userAttributes.put(key, value);
                    }
                } else {
                    userAttributes.put(key, value);
                }
            } else {
                userAttributes.put(key, value);
            }
        }
    }

    private void removeAttributes(Map<String, Object> attributes, List<String> attributesToRemove) {
        List<LinkedList<String>> attributesToRemoveNames = attributesToRemove.stream()
                .map(attributeName -> new LinkedList<>(Arrays.asList(attributeName.split("\\.")))).collect(Collectors.toList());
        for (LinkedList<String> attributeName : attributesToRemoveNames) {
            removeAttributes(attributes, attributeName);
        }
    }

    private void removeAttributes(Map<String, Object> userAttributes, LinkedList<String> attributeToRemoveName) {
        String attributeName = attributeToRemoveName.poll();
        if (attributeToRemoveName.isEmpty()) {
            userAttributes.remove(attributeName);
        } else {
            if (userAttributes.containsKey(attributeName)) {
                Object nestedUserAttribute = userAttributes.get(attributeName);
                if (nestedUserAttribute instanceof Map) {
                    removeAttributes((Map<String, Object>) nestedUserAttribute, attributeToRemoveName);
                }
            }
        }
    }

    private Map<String, Object> getUserData(SearchGuardRestClient client) throws InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException {
        return (Map<String, Object>) client.getUser(userName).getContent().get("data");
    }

}