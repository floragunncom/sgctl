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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

import org.apache.http.Header;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Format;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.commands.ConnectingCommand;
import com.google.common.io.Files;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Base command logic for adding or updating configuration variables.
 */
public abstract class AddOrUpdateConfigVar extends ConnectingCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Name of the variable")
    private String name;

    @Parameters(index = "1", description = "Value of the variable", arity = "0..1")
    private String value;

    @Option(names = { "-n", "--numeric-value" }, description = "Numeric value of the variable")
    private Number numericValue;

    @Option(names = { "-i",
            "--input-file" }, description = "Retrieve the variable value from an external file. Files ending with .json or .yml will be parsed as such. Other files will be treated as plain text.")
    private File inputFile;

    @Option(names = { "-e",
            "--encrypt" }, description = "If specified, the value will be encrypted server-side using the currently configured encryption key.")
    private boolean encrypt;

    @Option(names = { "--scope" })
    private String scope;

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {

            Object value = null;

            if (this.value != null) {
                value = this.value;
            } else if (this.numericValue != null) {
                value = this.numericValue;
            } else if (this.inputFile != null) {
                try {
                    Format format = Format.getByFileName(this.inputFile.getName(), null);
                    String fileContent = Files.asCharSource(this.inputFile, Charset.defaultCharset()).read();

                    if (verbose || debug) {
                        System.out.println("Uploading " + this.inputFile + " as " + (format != null ? format.getName() : "plain text"));
                    }

                    if (format != null) {
                        value = DocReader.format(format).read(fileContent);
                    } else {
                        value = fileContent;
                    }
                } catch (IOException | DocumentParseException e) {
                    throw new SgctlException("Error while reading " + this.inputFile + ": " + e.getMessage(), e);
                }
            }

            if (value == null) {
                throw new SgctlException("No value specified");
            }

            BasicResponse basicResponse = client.putConfigVar(name, value, scope, encrypt, getHeaders());
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

    /**
     * Headers to send to the config-var endpoint (e.g. conditional requests).
     */
    protected abstract Header[] getHeaders();

}
