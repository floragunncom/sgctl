/*
 * Copyright 2021-2022 floragunn GmbH
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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.client.api.GetBulkConfigResponse;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get-config", description = "Retrieves Search Guard configuration from the server to local files")
public class GetConfig extends ConnectingCommand implements Callable<Integer> {

    @Option(names = { "-o", "--output" }, required = true, arity = "1", description = "Directory to write configuration to")
    File outputDir;

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            if (debug || verbose) {
                System.out.println("Retrieving Search Guard configuration");
            }

            GetBulkConfigResponse response = client.getConfigBulk();

            if (!outputDir.exists()) {
                if (debug || verbose) {
                    System.out.println("Creating directory " + outputDir);
                }

                if (!outputDir.mkdirs()) {
                    throw new SgctlException("Could not create directory " + outputDir);
                }
            }

            for (GetBulkConfigResponse.ConfigDocument config : response) {
                if (!config.isExists()) {
                    continue;
                }
                
                File outputFile = new File(outputDir, config.getConfigType().getFileName());

                StringBuilder header = new StringBuilder();
                
                header.append("# sg_" + config.getConfigType().getApiName());
                header.append(" v:" + response.getSearchGuardVersion());
                
                if (getConnectedClusterName() != null) {
                    header.append(" cluster:" + getConnectedClusterName());                    
                }
                
                if (config.getEtag() != null) {
                    header.append(" etag:" + config.getEtag());
                }
                
                header.append("\n");
                
                try {
                    String result = header + config.getContent().toYamlString();
                    Files.asCharSink(outputFile, Charsets.UTF_8).write(result);
                } catch (IOException e) {
                    throw new SgctlException("Error while writing " + outputFile + ": " + e.getMessage(), e);
                }
            }

            System.out.println("Wrote configuration to " + outputDir);

            return 0;
        } catch (SgctlException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (InvalidResponseException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (FailedConnectionException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (ServiceUnavailableException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (UnauthorizedException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (ApiException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

}
