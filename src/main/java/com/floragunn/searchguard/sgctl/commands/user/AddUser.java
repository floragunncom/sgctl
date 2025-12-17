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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

/**
 * Command to create a new user via the Search Guard REST API.
 */
@Command(name = "add-user", description = "Adds a new user")
public class AddUser extends ConnectingCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "1", description = "User name")
    private String userName;

    @Option(names = { "-r", "--sg-roles" }, split = ",")
    private List<String> sgRoles;

    @Option(names = { "--backend-roles" }, split = ",")
    private List<String> backendRoles;

    @Option(names = { "-a", "--attributes" }, split = ",")
    private Map<String, Object> attributes;

    @Option(names = { "--password" }, arity = "0..1", description = "Passphrase", interactive = true, required = true)
    String password;

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {

            sgRoles = Optional.ofNullable(sgRoles).orElse(Collections.emptyList());
            backendRoles = Optional.ofNullable(backendRoles).orElse(Collections.emptyList());
            attributes = Optional.ofNullable(attributes).orElse(Collections.emptyMap());
            Map<String, Object> newUserData = new HashMap<>();
            newUserData.put("search_guard_roles", sgRoles);
            newUserData.put("backend_roles", backendRoles);
            newUserData.put("attributes", attributes);
            newUserData.put("password", password);

            if (verbose || debug) {
                System.out.println("Adding a new user: " + userName + ", with SG roles: " + String.join(",", sgRoles) + ", with backend roles: "
                        + String.join(",", backendRoles) + ", with attributes: "
                        + attributes.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
            }
            BasicResponse basicResponse = client.putUser(userName, newUserData);
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

}
