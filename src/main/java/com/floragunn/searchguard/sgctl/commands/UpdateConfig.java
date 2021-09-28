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

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocType;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.errors.FileDoesNotExist;
import com.floragunn.codova.validation.errors.ValidationError;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.client.api.ConfigType;
import com.google.common.collect.ImmutableMap;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "update-config", description = "Updates Search Guard configuration on the server from local files")
public class UpdateConfig extends ConnectingCommand implements Callable<Integer> {

    @Parameters
    List<File> files;

    @Override
    public Integer call() {
        Map<String, String> configTypeToFileMap = new HashMap<>();
        Map<String, Map<String, Map<String, Object>>> configTypeToConfigMap = new LinkedHashMap<>();

        try (SearchGuardRestClient client = getClient().debug(debug)) {

            if (files.size() == 1 && files.get(0).isDirectory()) {
                File dir = files.get(0);
                files = Arrays.asList(dir.listFiles((File fileDir, String name) -> name.startsWith("sg_") && name.endsWith(".yml")));

                if (files.size() == 0) {
                    throw new SgctlException("Directory " + dir + " does not contain any configuration files");
                }

                if (verbose || debug) {
                    System.out.println("Uploading config files from directory " + dir.getAbsolutePath() + ": "
                            + String.join(", ", files.stream().map((f) -> f.getName()).collect(toList())));
                }
            } else if (verbose || debug) {
                System.out.println("Uploading config files: " + String.join(", ", files.stream().map((f) -> f.getName()).collect(toList())));
            }

            for (File file : files) {
                try {
                    DocType docType = DocType.getByFileName(file.getName(), DocType.YAML);
                    DocNode content = DocNode.wrap(DocReader.type(docType).readObject(file));

                    ConfigType configType = ConfigType.getFor(file, content);

                    if (!configTypeToConfigMap.containsKey(configType.getApiName())) {
                        configTypeToConfigMap.put(configType.getApiName(), ImmutableMap.of("content", content));
                        configTypeToFileMap.put(configType.getApiName(), file.getPath());
                    } else {
                        validationErrors.add(new ValidationError(file.getPath(), "Configuration of type " + configType.getApiName()
                                + " is already specifed in file " + configTypeToConfigMap.get(configType.getApiName())));
                    }
                } catch (FileNotFoundException e) {
                    validationErrors.add(new FileDoesNotExist(file.getPath(), file));
                } catch (JsonProcessingException e) {
                    validationErrors.add(new ValidationError(file.getPath(), e.getMessage()).cause(e));
                } catch (IOException e) {
                    validationErrors.add(new ValidationError(file.getPath(), "Error while reading: " + e).cause(e));
                } catch (ConfigValidationException e) {
                    validationErrors.add(file.getPath(), e);
                }
            }

            validationErrors.throwExceptionForPresentErrors();

            BasicResponse basicResponse = client.putConfigBulk(configTypeToConfigMap);

            System.out.println(basicResponse.getMessage());

            return 0;
        } catch (ConfigValidationException e) {
            System.err.println("Invalid config files:\n" + e.getValidationErrors());
            return 1;
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
            if (e.getValidationErrors() != null) {
                Map<String, ValidationErrors> validationErrorsByFile = e.getValidationErrors().groupByKeys(configTypeToFileMap);

                System.err.println("Invalid config files:\n");

                for (Map.Entry<String, ValidationErrors> entry : validationErrorsByFile.entrySet()) {
                    System.err.println(entry.getKey() + ":");
                    System.err.println(entry.getValue().toString().replaceAll("(?m)^", "  "));
                    System.err.println();
                }
            } else {
                System.err.println(e.getMessage());
            }

            return 1;
        }
    }

}
