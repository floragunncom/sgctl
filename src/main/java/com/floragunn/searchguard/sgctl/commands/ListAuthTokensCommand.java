package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.client.*;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;
@Command(name = "list-auth-tokens", description = "List available auth tokens")


public class ListAuthTokensCommand  extends ConnectingCommand  implements Callable<Integer> {

    @Override
    public Integer call()  {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            BasicResponse response = client.listAuthTokens();
            System.out.println("List AuthToken Response: " + response);
            return CommandLine.ExitCode.OK;
        }
        catch (Exception e){
            System.out.println("Exception encountered while retrieving Auth token list: " + e);
            return CommandLine.ExitCode.SOFTWARE;
        }


    }
}
