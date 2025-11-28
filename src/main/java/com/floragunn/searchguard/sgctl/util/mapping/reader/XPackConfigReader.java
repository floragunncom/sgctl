package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
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
    MigrationReport report;

    public XPackConfigReader(File elasticsearch, File user, File role, File roleMappingFile) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.roleMappingFile = roleMappingFile;
        this.ir = new IntermediateRepresentation();
        this.report = MigrationReport.shared;
    }

    public IntermediateRepresentation generateIR() {
        new RoleConfigReader(roleFile, ir, report);
        new UserConfigReader(userFile, ir, report);
        new RoleMappingConfigReader(roleMappingFile, ir, report);

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
                    continue;
                }
                result.add(value);
            } else {
                MigrationReport.shared.addInvalidType(fileName, origin, LinkedHashMap.class, element);
            }
        }
        return result;
    }

    static List<String> toStringList(Object obj, String originFile, String parameterOrigin, String key) {
        try {
            return toStringList(obj);
        } catch (IllegalArgumentException e) {
            MigrationReport.shared.addInvalidType(originFile, parameterOrigin, List.class, obj);
        } catch (ClassCastException e) {
            MigrationReport.shared.addWarning(originFile, parameterOrigin + "->key", "Expected type 'String' for all items in this array but found " + e.getMessage());
        }
        return null;
    }

    private static List<String> toStringList(Object obj) throws IllegalArgumentException, ClassCastException {
        if (!(obj instanceof List<?> list)) {
            throw new IllegalArgumentException("Object is not a List");
        }

        ArrayList<String> result = new ArrayList<>();

        for (Object element : list) {
            if (!(element instanceof String)) {
                throw new ClassCastException();
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
