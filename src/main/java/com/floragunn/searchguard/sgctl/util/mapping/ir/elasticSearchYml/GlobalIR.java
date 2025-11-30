package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

public class GlobalIR {

    boolean xpackSecEnabled;

    public void handleGlobalOptions(String optionName, Object optionValue) {
        boolean error = false;

        // Booleans
        if (IntermediateRepresentationElasticSearchYml.assertType(optionValue, Boolean.class)) {
            boolean value = (Boolean) optionValue;
            switch (optionName) {
                case "enabled":
                    xpackSecEnabled = value;
            }
        }

    }
}
