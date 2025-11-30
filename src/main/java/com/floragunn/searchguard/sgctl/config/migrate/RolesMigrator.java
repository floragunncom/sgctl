package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import org.slf4j.Logger;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import com.floragunn.searchguard.sgctl.config.searchguard.SgInternalRoles;
import java.util.List;
import java.util.Optional;

public class RolesMigrator implements SubMigrator{


    public List<NamedConfig<?>> migrate(Migrator.IMigrationContext context, Logger logger)
            throws SgctlException{
        logger.info("Migrating Roles");
        Optional<Roles> xpackRoles = context.getRoles();
        if (xpackRoles.isEmpty()){
            logger.warn("roles.json is empty");
            return List.of();
        }

    }
}

