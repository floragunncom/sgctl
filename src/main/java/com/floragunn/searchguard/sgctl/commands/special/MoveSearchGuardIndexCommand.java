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

package com.floragunn.searchguard.sgctl.commands.special;

import java.util.concurrent.Callable;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.PreconditionFailedException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.commands.ConnectingCommand;

import picocli.CommandLine.Command;

@Command(name = "move-sg-index", description = "Moves the searchguard index to its new-style name")
public class MoveSearchGuardIndexCommand extends ConnectingCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            if (debug || verbose) {
                System.out.println("Moving searchguard index to new-style location");
            }

            BasicResponse response = client.post("/_searchguard/config/migrate_index").parseResponseBy(BasicResponse::new);
            System.out.println(response.getMessage());

            return 0;
        } catch (PreconditionFailedException e) {
            if (isAlreadyMigrated(e.getMessage())) {
                System.out.println(e.getMessage());
                return 0;
            }
            System.err.println(e.getMessage());
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
            System.err.println(e.getMessage());
            return 1;
        }
    }

    private boolean isAlreadyMigrated(String message) {
        return message != null && message.toLowerCase().contains("already uses the new-style index");
    }

}
