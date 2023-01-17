package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "revoke-auth-token", description = "Revoke specified auth token")

public class RevokeAuthTokenCommand extends ConnectingCommand  implements Callable<Integer> {

    @Parameters(index =  "0", arity = "1", description = "The ID of the auth token to be removed")
    String authTokenId;
    @Override
    public Integer call() throws Exception {

        try (SearchGuardRestClient client = getClient().debug(debug)) {
            BasicResponse response = client.revokeAuthToken(authTokenId);
            System.out.println("Remove AuthToken Response: " + response);
            return CommandLine.ExitCode.OK;

        }
        catch (Exception e) {
            System.out.println("Exception encountered while trying to revoke the specified Auth token " + authTokenId + " :" +  e);
            return CommandLine.ExitCode.SOFTWARE;
        }
    }
}
