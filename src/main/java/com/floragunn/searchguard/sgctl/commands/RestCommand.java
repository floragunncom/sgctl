package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.DocumentParseException;
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
    enum SupportedHttpMethods {get, put, delete, post, patch}

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
    public Integer call() throws Exception {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            SearchGuardRestClient.Response response;
            switch (httpMethod) {
                case get:
                    response = client.get(endpoint);
                    break;
                case delete:
                    response = client.delete(endpoint);
                    break;
                case put:
                    response = client.put(endpoint, evaluateBody(), ContentType.APPLICATION_JSON);
                    break;
                case patch:
                    response = client.patch(endpoint, evaluateBody(), ContentType.APPLICATION_JSON);
                case post:
                    if (jsonString == null && inputFilePath == null)
                        response = client.post(endpoint);
                    else
                        response = client.post(endpoint, evaluateBody(), ContentType.APPLICATION_JSON);
                    break;
                default:
                    throw new SgctlException("Unknown http method '" + httpMethod + "'");
            }
            BasicResponse basicResponse = response.parseResponseBy(BasicResponse::new);
            String responseString = DocWriter.json().pretty().writeAsString(basicResponse.getContent());
            System.out.println(responseString);
            if (outputFilePath != null) {
                handleFileOutput(responseString);
            }
            return 0;
        }
        catch (UnexpectedDocumentStructureException | DocumentParseException | IOException | SgctlException |
                FailedConnectionException | InvalidResponseException | UnauthorizedException | ServiceUnavailableException | ApiException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    private String evaluateBody() throws UnexpectedDocumentStructureException, DocumentParseException, IOException, SgctlException {
        if (jsonString != null) {
            return jsonString;
        }
        else if(inputFilePath != null) {
            return DocWriter.json().writeAsString(DocReader.json().readObject(inputFilePath));
        }
        throw new SgctlException("Http method '" + httpMethod + "' requires an input file or a JSON string");
    }

    private void handleFileOutput(String response) throws SgctlException {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
            writer.write(response);
            writer.close();
        } catch (IOException e) {
            throw new SgctlException("Error while writing " + outputFilePath.getPath() + ": " + e, e);
        }
    }
}
