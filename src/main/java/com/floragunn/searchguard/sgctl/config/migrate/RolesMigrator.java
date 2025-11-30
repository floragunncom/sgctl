package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import org.slf4j.Logger;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalRoles;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
public class RolesMigrator implements SubMigrator{


    public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger)
            throws SgctlException{
        logger.info("Migrating Roles");
        Optional<Roles> xpackRoles = context.getRoles();
        if (xpackRoles.isEmpty()){
            logger.warn("roles.json is empty");
            return List.of();
        }
        //todo return
        return null;
    }



    //todo names -> index pattern
    //todo priivileges -> allowed actions
    //todo renaming cluster privileges content
    /**
     * all ->	SGS_CLUSTER_ALL
     * createsnapshot -> SGS\MANAGE_SNAPSHOTS
     * manageindextemplates	-> SGS_CLUSTER_MANAGE_INDEX_TEMPLATES
     * manageingestpipelines ->	SGS_CLUSTER_MANAGE_PIPELINES
     * monitor -> SGS_CLUSTER_MONITOR
     */

    //todo renaming indices privileges content
    /**
     * all ->	SGS_INDICES_ALL
     * create ->	SGS_CREATE_INDEX
     * createindex -> SGS\CREATE_INDEX
     * delete ->	SGS_DELETE
     * index ->	SGS_WRITE
     * manage ->	SGS_MANAGE
     * monitor ->	SGS_INDICES_MONITOR
     * read	-> SGS_READ
     * write -> SGS_WRITE
     */


}

