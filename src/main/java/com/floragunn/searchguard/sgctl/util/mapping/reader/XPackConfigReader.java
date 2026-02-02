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


package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * Aggregates X-Pack configuration files into a unified intermediate representation.
 */
public class XPackConfigReader {

    private final File elasticsearch;
    private final File userFile;
    private final File roleFile;
    private final File roleMappingFile;

    public XPackConfigReader(File elasticsearch, File user, File role, File roleMappingFile) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.roleMappingFile = roleMappingFile;
    }

    /**
     * Reads all configured files and produces an {@link IntermediateRepresentation}.
     */
    public IntermediateRepresentation generateIR() {
        IntermediateRepresentation ir = new IntermediateRepresentation();
        readConfig(RoleConfigReader.FILE_NAME, () -> new RoleConfigReader(roleFile, ir));
        readConfig(UserConfigReader.FILE_NAME, () -> new UserConfigReader(userFile, ir));
        readConfig(RoleMappingConfigReader.FILE_NAME, () -> new RoleMappingConfigReader(roleMappingFile, ir));
        try {
            new ElasticsearchYamlReader(elasticsearch, ir.getElasticSearchYml());
        } catch (Exception e) {
            MigrationReport.shared.addWarning("elasticsearch.yml", "origin", safeMessage(e));
        }

        return ir.freeze();
    }

    private void readConfig(String fileName, ReaderAction action) {
        try {
            action.run();
        } catch (DocumentParseException | IOException e) {
            MigrationReport.shared.addWarning(fileName, "origin", safeMessage(e));
        }
    }

    private String safeMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    @FunctionalInterface
    private interface ReaderAction {
        void run() throws DocumentParseException, IOException;
    }

    static <T> List<T> readList(List<?> rawList, Function<LinkedHashMap<?, ?>, T> reader, String fileName, String origin) {
        var result = new ArrayList<T>();
        for (var element : rawList) {
            if (element instanceof LinkedHashMap<?, ?> rawMap) {
                var value = reader.apply(rawMap);
                if (value == null) {
                    return null;
                }
                result.add(value);
            } else {
                MigrationReport.shared.addInvalidType(fileName, origin, LinkedHashMap.class, element);
            }
        }
        return result;
    }

    static List<String> toStringList(Object obj, String originFile, String parameterOrigin, String key) {
        if (!(obj instanceof List<?> list)) {
            MigrationReport.shared.addInvalidType(originFile, parameterOrigin, List.class, obj);
            return null;
        }

        ArrayList<String> result = new ArrayList<>(list.size());

        for (Object element : list) {
            if (!(element instanceof String)) {
                MigrationReport.shared.addInvalidType(originFile, parameterOrigin + "->" + key, String.class, element);
                return null;
            }
            result.add((String) element);
        }

        return result;
    }
}
