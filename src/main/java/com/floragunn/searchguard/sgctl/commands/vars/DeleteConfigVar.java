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

package com.floragunn.searchguard.sgctl.commands.vars;

import java.util.concurrent.Callable;

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
import picocli.CommandLine.Parameters;

@Command(name = "delete-var", description = "Deletes an existing configuration variable")
public class DeleteConfigVar extends ConnectingCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Name of the variable")
    private String name;

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            BasicResponse basicResponse = client.deleteConfigVar(name);
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
