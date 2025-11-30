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

public class XPackConfigReader {

    File elasticsearch;
    File userFile;
    File roleFile;
    File roleMappingFile;
    IntermediateRepresentation ir;

    public XPackConfigReader(File elasticsearch, File user, File role, File roleMappingFile) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.roleMappingFile = roleMappingFile;
        this.ir = new IntermediateRepresentation();
    }

    public IntermediateRepresentation generateIR() throws DocumentParseException, IOException {
        new RoleConfigReader(roleFile, ir);
        new UserConfigReader(userFile, ir);
        new RoleMappingConfigReader(roleMappingFile, ir);

        ir.getUsers().forEach(user -> print(user.toString()));
        ir.getRoles().forEach(role -> print(role.toString()));
        ir.getRoleMappings().forEach(roleMapping -> print(roleMapping.toString()));

        return ir;
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

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
