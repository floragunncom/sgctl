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

@Command(name = "set", description = "Modifies a property in the Search Guard Configuration")
public class SetCommand extends ConnectingCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Type of the configuration to be modified. Example: authc")
    String configType;

    @Parameters(index = "1", description = "Path to the property to the modified. Example: network.trusted_proxies")
    String propertyPath;

    @Parameters(index = "2", arity = "0..1", description = "Set the property to the given string value")
    String value;

    @Option(names = { "-n", "--numeric-value" }, description = "Set the property to the given numeric value")
    Number numericValue;

    @Option(names = { "--true" }, description = "Set the property to the boolean value true")
    boolean booleanTrue;

    @Option(names = { "--false" }, description = "Set the property to the boolean value false")
    boolean booleanFalse;

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

}
