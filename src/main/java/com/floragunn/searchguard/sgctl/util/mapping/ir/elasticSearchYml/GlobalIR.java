package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

import java.io.File;

/**
 * Intermediate representation for global xpack.security settings from elasticsearch.yml.
 */
public class GlobalIR {

    private static final String FILE_NAME = "elasticsearch.yml";

    private boolean xpackSecEnabled;

    public boolean getXpackSecEnabled() { return xpackSecEnabled; }

    public void handleGlobalOptions(String optionName, Object optionValue, String keyPrefix, File configFile) {
        boolean keyKnown = true;

        // Booleans
        if (IntermediateRepresentationElasticSearchYml.isType(optionValue, Boolean.class)) {
            boolean value = (Boolean) optionValue;
            switch (optionName) {
                case "enabled":
                    xpackSecEnabled = value;
                    break;
                default:
                    keyKnown = false;
            }
        }

        if (!keyKnown) {
            MigrationReport.shared.addUnknownKey(FILE_NAME, keyPrefix + optionName, configFile.getPath());
        }
    }
}
