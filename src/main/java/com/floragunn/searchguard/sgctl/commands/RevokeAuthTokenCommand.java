package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "revoke-auth-tokens", description = "Revoke specified auth token")


public class RevokeAuthTokensCommand extends ConnectingCommand  implements Callable<Integer> {

    @Parameters(index =  "0", arity = "1", description = "The ID of the auth token to be removed")
    String authTokenId;
    @Override
    public Integer call() throws Exception {


        try (SearchGuardRestClient client = getClient().debug(debug)) {
            BasicResponse response = client.revokeAuthToken(authTokenId);
            System.out.println("Remove AuthToken Response: " + response);
            return 0;

        }
        catch (NumberFormatException e) { //TODO Specific exception catching
            System.out.println("error " + e);
            return 1;
        }


    }
}
