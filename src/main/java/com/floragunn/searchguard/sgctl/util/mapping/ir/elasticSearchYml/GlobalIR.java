/*
 * Copyright 2025-2026 floragunn GmbH
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
