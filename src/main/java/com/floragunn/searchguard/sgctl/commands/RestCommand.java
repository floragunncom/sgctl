package com.floragunn.searchguard.sgctl.commands;

import java.util.concurrent.Callable;

import com.floragunn.codova.documents.patch.DocPatch;
import com.floragunn.codova.documents.patch.SimplePathPatch;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rest", description = "Sends an REST request with admin authorization to the cluster")
public class RestCommand extends ConnectingCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "REST method to be used")
    Method method;

    @Parameters(index = "1", description = "Request path")
    String path;

    @Parameters(index = "2", arity = "0..1", description = "Set the property to the given string value")
    String value;

    @Option(names = { "-c", "--content-type" }, description = "The value of the Content-Type header to be set")
    String contentType;

    @Option(names = { "--json" }, description = "Request body in JSON format")
    String json;
    
    @Override
    public Integer call() throws Exception {
        try (SearchGuardRestClient client = getClient().debug(debug)) {

            Object value = booleanTrue ? Boolean.TRUE : booleanFalse ? Boolean.FALSE : numericValue != null ? numericValue : this.value;

            if (value == null) {
                System.err.println("Value is missing");
                return 1;
            }

            DocPatch docPatch = new SimplePathPatch(new SimplePathPatch.Operation(propertyPath, value));

            BasicResponse basicResponse = client.patch("/_searchguard/config/" + configType, docPatch).parseResponseBy(BasicResponse::new);

            System.out.println(basicResponse.getMessage());

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
            if (e.getValidationErrors() != null) {
                System.err.println("Modified configuration is not valid. Update rejected.\n" + e.getValidationErrors().toString());
            } else {
                System.err.println(e.getMessage());
            }

            return 1;
        }
    }
    
    public static enum Method {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH
    }
}
