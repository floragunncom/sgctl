package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

import java.io.File;

public class GlobalIR {

    boolean xpackSecEnabled;

    String THIS_FILE = "elasticsearch.yml";
    public void handleGlobalOptions(String optionName, Object optionValue, String keyPrefix, File configFile) {
        //boolean error = false;
        boolean keyKnown = true;

        // Booleans
        if (IntermediateRepresentationElasticSearchYml.assertType(optionValue, Boolean.class)) {
            boolean value = (Boolean) optionValue;
            switch (optionName) {
                case "enabled":
                    xpackSecEnabled = value;
                    break;
                default:
                    keyKnown = false;
            }
        }

        if (keyKnown) {
            MigrationReport.shared.addMigrated(THIS_FILE, keyPrefix + optionName);
        } else {
            MigrationReport.shared.addUnknownKey(THIS_FILE, keyPrefix + optionName, configFile.getPath());
        }
    }
}
