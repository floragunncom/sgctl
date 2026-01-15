package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;
import com.floragunn.searchguard.sgctl.util.mapping.writer.SGAuthcWriter;

import java.util.*;

public abstract class RealmTranslator {
    public static final String SG_AUTHC_FILE_NAME = SGAuthcWriter.SG_AUTHC_FILE_NAME;
    public static final String SG_FRONTEND_AUTHC_FILE_NAME = SGAuthcWriter.SG_FRONTEND_AUTHC_FILE_NAME;
    protected boolean isFrontEnd = false;
    protected final Map <String, Object> config = new HashMap<>();

    protected void realmNotImplementedReport(RealmIR realm) {
        MigrationReport.shared.addManualAction(isFrontEnd ? SG_FRONTEND_AUTHC_FILE_NAME : SG_AUTHC_FILE_NAME, realm.getName(), String.format("Realm migration for type %s not yet implemented.", realm.getType()));
    }

    public static void unknownRealmReport(RealmIR realm) {
        MigrationReport.shared.addManualAction(SG_AUTHC_FILE_NAME, realm.getName(), String.format("Realm migration for type %s is not known.", realm.getType()));
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
        if (value == null || (value instanceof String string && string.isEmpty()) || (value instanceof List<?> list && list.isEmpty()))
            return;
        config.put(key, value);

        String fileName;
        if (isFrontEnd) {
            fileName = SG_FRONTEND_AUTHC_FILE_NAME;
        } else {
            fileName = SG_AUTHC_FILE_NAME;
        }
        //TODO Decide if we want to keep this or just the realms
        MigrationReport.shared.addMigrated(fileName, key);


    }

    protected void addOptionalConfigProperty(String key, Object value) {
        addOptionalConfigProperty(key, value, null);
    }
    public abstract NewAuthDomain translate(RealmIR originalIR);

    public record NewAuthDomain(String frontendType, String backendType, Map<String, Object> frontendConfig,
                         Map<String, Object> backendConfig) implements Document<NewAuthDomain> {

        public NewAuthDomain(String frontendType, Map<String, Object> frontendConfig) {
                this(frontendType, null, frontendConfig, null);
            }

            @Override
            public Object toBasicObject() {
                LinkedHashMap<String, Object> result = new LinkedHashMap<>();

                result.put("type", backendType != null ? (frontendType + "/" + backendType) : frontendType);

                if (frontendConfig != null && !frontendConfig.isEmpty()) {
                    result.put(frontendType, frontendConfig);
                }

                if (backendConfig != null && !backendConfig.isEmpty()) {
                    result.put(backendType, backendConfig);
                }

                return result;
            }
        }
}
