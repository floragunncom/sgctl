package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;
@Command(name = "list-auth-tokens", description = "List available auth tokens")
public class ListAuthTokensCommand  extends ConnectingCommand  implements Callable<Integer> {


    @Override
    public Integer call() throws Exception {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            System.out.println("Sam : ");
            BasicResponse response = client.listAuthTokens();
            System.out.println("ListAuthToken Response: " + response);
            return 0;

        }
        catch (NumberFormatException e) { //TODO Specific exception catching
            System.out.println("error " + e);
            return 1;
        }


    }
}
