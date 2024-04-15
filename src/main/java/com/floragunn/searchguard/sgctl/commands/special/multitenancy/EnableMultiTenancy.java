/*
 * Copyright 2024 floragunn GmbH
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

package com.floragunn.searchguard.sgctl.commands.special.multitenancy;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.commands.ConnectingCommand;
import org.apache.http.entity.ContentType;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "enable-mt", description = "Turns on multi-tenancy")
public class EnableMultiTenancy extends ConnectingCommand implements Callable<Integer> {

    private static final String ENDPOINT_PATH = "/_searchguard/config/fe_multi_tenancy/activation";

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            if (debug || verbose) {
                System.out.println("Turning on multi-tenancy");
            }

            BasicResponse response = client.put(ENDPOINT_PATH, "", ContentType.APPLICATION_JSON).parseResponseBy(BasicResponse::new);

            System.out.println(response.toPrettyJsonString());

            return 0;
        } catch (SgctlException | ApiException | InvalidResponseException | ServiceUnavailableException |
                 FailedConnectionException | UnauthorizedException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

}
