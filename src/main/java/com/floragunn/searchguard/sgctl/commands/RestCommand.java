package com.floragunn.searchguard.sgctl.commands;

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
import org.apache.http.entity.ContentType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;



@Command(name = "rest", description = "REST client for administration")
public class RestCommand extends ConnectingCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "1", description = "http method")
    SupportedHttpMethods httpMethod;

    @Parameters(index = "1", arity = "1", description = "Endpoint path")
    String endpoint;

    @Option(names = {"-i", "--input"}, description = "Path to a file")
    File inputFilePath;

    @Option(names = {"--json"}, description = "JSON string")
    String jsonString;

    @Option(names = {"-o", "--output"}, description = "Custom output file path")
    File outputFilePath;

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            BasicResponse basicResponse = httpMethod.handle(client, endpoint, jsonString, inputFilePath);
            String responseString = DocWriter.json().pretty().writeAsString(basicResponse.getContent()); //DocWriter does not support pretty printing to file
            System.out.println(responseString);
            handleFileOutput(outputFilePath, responseString);
            return 0;
        }
        catch (SgctlException | UnauthorizedException | ApiException | InvalidResponseException | ServiceUnavailableException | FailedConnectionException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    private static void handleFileOutput(File outputFilePath, String response) throws SgctlException {
        if (outputFilePath == null) return;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
            writer.write(response);
            writer.close();
        }
        catch (IOException e) {
            throw new SgctlException("Error while writing " + outputFilePath.getPath() + ": " + e, e);
        }
    }

    private enum SupportedHttpMethods {
        GET("get",
                Input::validateNoInput,
                (client, endpoint, evaluatedInput) -> client.get(endpoint)),
        PUT("put",
                input -> input.validateExistent().validateNoDuplicate(),
                (client, endpoint, evaluatedInput) -> client.put(endpoint, evaluatedInput.getContent(), evaluatedInput.getContentType())),
        DELETE("delete",
                Input::validateNoInput,
                (client, endpoint, evaluatedInput) -> client.delete(endpoint)),
        POST("post",
                Input::validateNoDuplicate,
                (client, endpoint, evaluatedInput) -> evaluatedInput == null ? client.post(endpoint) : client.post(endpoint, evaluatedInput.getContent(), evaluatedInput.getContentType())),
        PATCH("patch",
                input -> input.validateExistent().validateNoDuplicate(),
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

        public BasicResponse handle(SearchGuardRestClient client, String endpoint, String jsonString, File inputFilePath)
                throws SgctlException, FailedConnectionException, InvalidResponseException, UnauthorizedException, ServiceUnavailableException, ApiException {
            Input input = Input.create(jsonString, inputFilePath);
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
            public static Input create(String jsonString, File inputFilePath) {
                return new Input(jsonString, inputFilePath);
            }

            private String jsonString;
            private File inputFilePath;

            private Input(String jsonString, File inputFilePath) {
                this.jsonString = jsonString;
                this.inputFilePath = inputFilePath;
            }

            public Input validateNoInput() throws SgctlException {
                if (!isEmpty()) System.out.println("No input required. Input is ignored");
                jsonString = null;
                inputFilePath = null;
                return this;
            }

            public Input validateNoDuplicate() throws SgctlException {
                if (jsonString != null && inputFilePath != null)
                    throw new SgctlException("Two inputs defined. Choose either '--json' or '--input'");
                return this;
            }

            public Input validateExistent() throws SgctlException {
                if (jsonString == null && inputFilePath == null)
                    throw new SgctlException("This method requires an input. Use '--json' or '--input' to define an input");
                return this;
            }

            public boolean isEmpty() {
                return jsonString == null && inputFilePath == null;
            }

            public EvaluatedInput evaluate() throws SgctlException {
                if (isEmpty()) return null;
                if (jsonString != null) return new EvaluatedInput(jsonString, ContentType.create(Format.JSON.getMediaType()));
                //TODO: Check if format is supported
                Format format = Format.getByFileName(inputFilePath.getName(), Format.JSON);
                try {
                    String content = DocWriter.format(format).writeAsString(DocReader.format(format).readObject(inputFilePath));
                    return new EvaluatedInput(content, ContentType.create(format.getMediaType()));
                }
                catch (UnexpectedDocumentStructureException | DocumentParseException | IOException e) {
                    throw new SgctlException("Could not read file from path '" + inputFilePath + "' " + e, e);
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
