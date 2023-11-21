package com.floragunn.searchguard.sgctl.commands.special.multitenancy.datamigration880;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.BasicResponse;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.commands.ConnectingCommand;
import org.apache.http.entity.ContentType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "start-mt-data-migration-8.7-to-8.8", description = "Starts migration of multi-tenancy data from Kibana 8.7 to 8.8")
public class StartMultiTenancyDataMigration extends ConnectingCommand implements Callable<Integer> {

    private static final String ENDPOINT_PATH = "/_searchguard/config/fe_multi_tenancy/data_migration/8_8_0";
    private static final String FIELD_ALLOW_YELLOW_INDICES = "allow_yellow_indices";

    @Option(names = {"--allow-yellow-indices"}, description = "If specified, data migration will run even if data indices or backup indices are in yellow state")
    private boolean allowYellowIndices;

    @Override
    public Integer call() {
        try (SearchGuardRestClient client = getClient().debug(debug)) {
            final String reqBody = DocNode.of(FIELD_ALLOW_YELLOW_INDICES, allowYellowIndices).toPrettyJsonString();

            if (debug || verbose) {
                System.out.println("Starting multi tenancy data migration from Kibana 8.7 to 8.8");
                System.out.println("Request body: " + reqBody);
            }

            BasicResponse response = client.post(ENDPOINT_PATH, reqBody, ContentType.APPLICATION_JSON)
                    .parseResponseBy(BasicResponse::new);

            System.out.println(response.toPrettyJsonString());

            return 0;
        } catch (SgctlException | ApiException | InvalidResponseException | ServiceUnavailableException |
                 FailedConnectionException | UnauthorizedException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
