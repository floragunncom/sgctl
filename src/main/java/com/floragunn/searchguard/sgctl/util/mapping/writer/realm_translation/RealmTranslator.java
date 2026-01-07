package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

import java.util.HashMap;
import java.util.Map;

public abstract class RealmTranslator {
    public static final String SG_AUTHC_FILE_NAME = "sg_authc.yml";
    public static final String SG_FRONTEND_AUTHC_FILE_NAME = "sg_frontend_authc.yml";
    protected boolean isFrontEnd = false;
    protected final Map <String, Object> config = new HashMap<>();

    public static void realmNotImplementedReport(String realmName, RealmIR realm) {
        MigrationReport.shared.addManualAction(SG_AUTHC_FILE_NAME, realmName, String.format("Realm migration for type %s not yet implemented.", realm.getType()));

    }

    public boolean getIsFrontEnd() {
        return isFrontEnd;
    }

    protected String toBasicType(String type) {
        return String.format("basic/%s", type);
    }

    /**
     * Optionally adds a value to a config if value is not null
     * @param key The Key which needs to be added
     * @param value Optional value that gets added if present
     */
    protected void addOptionalConfigProperty(String key, Object value, Object defaultValue) {
        //We want to just add something that holds actual information
        if (value == null)
            value = defaultValue;
        if (value == null || (value instanceof String string && string.isEmpty()))
            return;
        config.put(key, value);

        String fileName;
        if (isFrontEnd) {
            fileName = SG_FRONTEND_AUTHC_FILE_NAME;
        } else {
            fileName = SG_AUTHC_FILE_NAME;
        }
        MigrationReport.shared.addMigrated(fileName, key);


    }

    protected void addOptionalConfigProperty(String key, Object value) {
        addOptionalConfigProperty(key, value, null);
    }
    public abstract MigrateConfig.NewAuthDomain translate(RealmIR originalIR);
}
