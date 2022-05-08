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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.floragunn.codova.documents.UnexpectedDocumentStructureException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.searchguard.sgctl.SgctlConfig;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.PreconditionFailedException;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import picocli.CommandLine.Option;

public class BaseCommand {

    static final File DEFAULT_CONFIG_DIR = new File(System.getProperty("user.home"), ".searchguard");

    @Option(names = { "-c", "--cluster" }, description = "The ID of the cluster configuration to be used by this command")
    String clusterIdOption;

    @Option(names = { "--debug" }, description = "Print debug information")
    protected boolean debug;

    @Option(names = { "-v", "--verbose" }, description = "Print more information")
    protected boolean verbose;

    @Option(names = { "--sgctl-config-dir" }, description = "The directory where sgctl reads from and writes to its configuration")
    File customConfigDir;

    protected final ValidationErrors validationErrors = new ValidationErrors();

    private String selectedClusterId;
    private boolean selectedClusterIdInitialized;

    protected String getSelectedClusterId() throws SgctlException {
        if (!selectedClusterIdInitialized) {
            if (clusterIdOption != null) {
                selectedClusterId = clusterIdOption;
                selectedClusterIdInitialized = true;
            } else {
                File configFile = new File(getConfigDir(), "sgctl-selected-config.txt");

                try {
                    selectedClusterId = Files.asCharSource(configFile, Charsets.UTF_8).readFirstLine();
                    selectedClusterIdInitialized = true;

                    if (verbose || debug) {
                        System.out.println("Selected cluster: " + selectedClusterId);
                    }

                } catch (FileNotFoundException e) {
                    return null;
                } catch (IOException e) {
                    throw new SgctlException("Error while reading " + configFile, e);
                }
            }
        }

        return selectedClusterId;
    }

    protected void writeSelectedClusterId(String selectedClusterId) throws SgctlException {
        File configFile = new File(getConfigDir(), "sgctl-selected-config.txt");

        try {
            Files.asCharSink(configFile, Charsets.UTF_8).write(selectedClusterId);
        } catch (IOException e) {
            throw new SgctlException("Error while writing " + selectedClusterId);
        }
    }

    protected SgctlConfig.Cluster getSelectedClusterConfig() throws SgctlException {
        String selectedClusterId = getSelectedClusterId();

        if (selectedClusterId == null || "none".equals(selectedClusterId)) {
            return null;
        }

        return SgctlConfig.Cluster.read(getConfigDir(), selectedClusterId);
    }

    protected File getConfigDir() throws SgctlException {
        if (customConfigDir != null) {
            if (customConfigDir.isFile()) {
                throw new SgctlException("The path specified by --sgctl-config-dir must be a directory");
            }
            return customConfigDir;
        } else {
            return DEFAULT_CONFIG_DIR;
        }
    }

    protected void retryOnConcurrencyConflict(RetryableProcedure retryableProcedure) throws SgctlException, InvalidResponseException,
            FailedConnectionException, ServiceUnavailableException, UnauthorizedException, ApiException, UnexpectedDocumentStructureException {
        int maxRetries = 3;
        int retry = 1;

        for (;;) {
            try {
                retryableProcedure.run();
                break;
            } catch (PreconditionFailedException e) {
                retry++;

                if (retry > maxRetries) {
                    throw new PreconditionFailedException(
                            "Could not perform operation due to concurrency conflict. Retried " + maxRetries + " times. Giving up now.", e);
                }
            }
        }
    }

    @FunctionalInterface
    protected static interface RetryableProcedure {
        void run() throws SgctlException, InvalidResponseException, FailedConnectionException, ServiceUnavailableException, UnauthorizedException,
                ApiException, UnexpectedDocumentStructureException;
    }

}
