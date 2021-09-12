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

package com.floragunn.searchguard.sgctl.commands;

import java.util.concurrent.Callable;

import com.floragunn.searchguard.sgctl.SgctlConfig;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "connect", description = "Tries to connect to a cluster and persists this connection for subsequent commands")
public class Connect extends ConnectingCommand implements Callable<Integer> {
    @Parameters(index = "0", arity = "0..1")
    String server;

    @Override
    public Integer call() {

        try (SearchGuardRestClient client = getClient()) {
            SgctlConfig.Cluster cluster = new SgctlConfig.Cluster(client.getHttpHost().getHostName(), client.getHttpHost().getPort(),
                    client.getTlsConfig());

            String clusterConfigId = getSelectedClusterId();

            if (clusterConfigId == null) {
                clusterConfigId = host;
            }

            cluster.setClusterId(clusterConfigId);

            cluster.write(getConfigDir());

            writeSelectedClusterId(clusterConfigId);

            return 0;
        } catch (SgctlException e) {
            System.err.println(e.getMessage());

            if (debug) {
                e.printStackTrace();
            }

            return 1;
        }
    }

    @Override
    protected String getHost() {
        if (host != null) {
            return host;
        } else {
            return server;
        }
    }

}
