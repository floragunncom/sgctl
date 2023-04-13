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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.http.entity.ContentType;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Format;
import com.floragunn.codova.documents.UnexpectedDocumentStructureException;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.util.ClonParser;
import com.google.common.io.Files;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rest", description = "REST client for administration")
public class RestCommand extends ConnectingCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "1", description = "http method")
    SupportedHttpMethods httpMethod;

    @Parameters(index = "1", arity = "1", description = "Endpoint path")
    String endpoint;

    @Option(names = { "-i", "--input" }, description = "Path to a file")
    File inputFilePath;

    @Option(names = { "--json" }, description = "JavaScript Object Notation string")
    String jsonString;

    @Option(names = { "--clon" }, arity = "1..*", description = "Command Line Object Notation string")
    List<String> clonExpressions;

    @Option(names = { "-o", "--output" }, description = "Custom output file path")
    File outputFilePath;

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            BasicResponse basicResponse = httpMethod.handle(client, endpoint, jsonString, inputFilePath, clonExpressions);
            System.out.println(basicResponse.toString());
            handleFileOutput(outputFilePath, basicResponse);
            return 0;
        } catch (SgctlException | UnauthorizedException | ApiException | InvalidResponseException | ServiceUnavailableException
                | FailedConnectionException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    private static void handleFileOutput(File outputFilePath, BasicResponse response) throws SgctlException {
        if (outputFilePath == null) {
            return;
        }
        String fileSuffix = "text/plain".equals(response.getContentType()) ? "txt" : "json";
        String fileContent = "text/plain".equals(response.getContentType()) ? response.toString() : response.toPrettyJsonString();

        File file = outputFilePath;
        if (outputFilePath.isDirectory()) {
            file = new File(outputFilePath, "response-" + new SimpleDateFormat("yyyy-MM-dd--HH-mm").format(new Date()) + "." + fileSuffix);
        } 
        
        try {
            Files.asCharSink(file, Charset.defaultCharset()).write(fileContent);
        } catch (IOException e) {
            throw new SgctlException("Error while writing output to " + file.getPath() + ": " + e, e);
        }
    }

    private enum SupportedHttpMethods {
        GET("get", Input::validateEmpty, (client, endpoint, evaluatedInput) -> client.get(endpoint)),
        PUT("put", input -> input.validateExistent().validateNoDuplicate(),
                (client, endpoint, evaluatedInput) -> client.put(endpoint, evaluatedInput.getContent(), evaluatedInput.getContentType())),
        DELETE("delete", Input::validateEmpty, (client, endpoint, evaluatedInput) -> client.delete(endpoint)),
        POST("post", Input::validateNoDuplicate,
                (client, endpoint, evaluatedInput) -> evaluatedInput == null ? client.post(endpoint)
                        : client.post(endpoint, evaluatedInput.getContent(), evaluatedInput.getContentType())),
        PATCH("patch", input -> input.validateExistent().validateNoDuplicate(),
                (client, endpoint, evaluatedInput) -> client.patch(endpoint, evaluatedInput.getContent(), evaluatedInput.getContentType()));

        private final String name;
        private final InputValidator validator;
        private final SupportedHttpMethodHandler handler;

        SupportedHttpMethods(String name, InputValidator validator, SupportedHttpMethodHandler handler) {
            this.name = name;
            this.validator = validator;
            this.handler = handler;
        }

        @Override
        public String toString() {
            return name;
        }

        public BasicResponse handle(SearchGuardRestClient client, String endpoint, String jsonString, File inputFilePath,
                List<String> clonExpressions) throws SgctlException, FailedConnectionException, InvalidResponseException, UnauthorizedException,
                ServiceUnavailableException, ApiException {
            Input input = Input.create(jsonString, inputFilePath, clonExpressions);
            SearchGuardRestClient.Response response = handler.handle(client, endpoint, validator.validate(input).evaluate());
            return response.parseResponseBy(BasicResponse::new);
        }

        interface SupportedHttpMethodHandler {
            SearchGuardRestClient.Response handle(SearchGuardRestClient client, String endpoint, Input.EvaluatedInput evaluatedInput)
                    throws SgctlException, FailedConnectionException, InvalidResponseException;
        }

        interface InputValidator {
            Input validate(Input input) throws SgctlException;
        }

        private static class Input {
            public static Input create(String jsonString, File inputFilePath, List<String> clonExpressions) {
                return new Input(jsonString, inputFilePath, clonExpressions);
            }

            private String jsonString;
            private File inputFilePath;
            private List<String> clonExpressions;

            private Input(String jsonString, File inputFilePath, List<String> clonExpressions) {
                this.jsonString = jsonString;
                this.inputFilePath = inputFilePath;
                this.clonExpressions = clonExpressions;
            }

            public Input validateEmpty() throws SgctlException {
                if (!isEmpty()) {
                    System.err.println("No input required for this HTTP method. Input is ignored");
                }
                jsonString = null;
                inputFilePath = null;
                clonExpressions = null;
                return this;
            }

            public Input validateNoDuplicate() throws SgctlException {
                if ((jsonString != null && inputFilePath != null) || (inputFilePath != null && clonExpressions != null)
                        || (clonExpressions != null && jsonString != null)) {
                    throw new SgctlException("Only one input required. Choose '--json', '--input' or '--clon'");
                }
                return this;
            }

            public Input validateExistent() throws SgctlException {
                if (jsonString == null && inputFilePath == null && clonExpressions == null) {
                    throw new SgctlException("This HTTP method requires an input. Use '--json', '--input' or '--clon' to define an input");
                }
                return this;
            }

            public boolean isEmpty() {
                return jsonString == null && inputFilePath == null && clonExpressions == null;
            }

            public EvaluatedInput evaluate() throws SgctlException {
                if (isEmpty()) {
                    return null;
                }
                try {
                    final Format format = jsonString != null || clonExpressions != null ? Format.JSON : Format.getByFileName(inputFilePath.getName());
                    final Object content = clonExpressions != null ? ClonParser.parse(clonExpressions)
                            : jsonString != null ? DocReader.format(format).read(jsonString)
                                    : DocReader.format(format).readObject(inputFilePath);
                    return new EvaluatedInput(DocWriter.format(format).writeAsString(content), ContentType.create(format.getMediaType()));
                } catch (UnexpectedDocumentStructureException | DocumentParseException | IOException | Format.UnknownDocTypeException e) {
                    throw new SgctlException(
                            (jsonString != null ? "JSON input is invalid" : "Could not read file from path '" + inputFilePath + "' ") + "\n" + e, e);
                } catch (ClonParser.ClonException e) {
                    throw new SgctlException("CLON input invalid: " + e, e);
                }
            }

            private static class EvaluatedInput {
                private final String content;
                private final ContentType contentType;

                protected EvaluatedInput(String contentString, ContentType contentType) {
                    this.content = contentString;
                    this.contentType = contentType;
                }

                public String getContent() {
                    return content;
                }

                public ContentType getContentType() {
                    return contentType;
                }
            }
        }
    }
}
