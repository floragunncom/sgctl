/*
 * Copyright 2022 floragunn GmbH
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

package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.*;
import com.floragunn.searchguard.sgctl.client.api.GetSgLicenseResponse;
import com.google.common.io.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "update-license", description = "Updates the SG license")
public class UpdateSgLicense extends ConnectingCommand implements Callable<Integer> {

    @Option(names = {"-l", "--license"}, required = true, description = "Path to the license file")
    File licenseFile;

    @Override
    public Integer call() throws Exception {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            String licenseString = Files.asCharSource(licenseFile, StandardCharsets.UTF_8).read();
            Map<String, Object> body = new HashMap<>();
            body.put("key", licenseString);

            BasicResponse response = client.putSgLicense(body);
            System.out.println(response.getMessage());
            GetSgLicenseResponse getResponse = client.getSgLicense();
            System.out.println("Success. This license will expire on " + getResponse.getExpiryString());
            return 0;
        } catch (SgctlException | InvalidResponseException | FailedConnectionException | ServiceUnavailableException | UnauthorizedException | IOException e) {
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
