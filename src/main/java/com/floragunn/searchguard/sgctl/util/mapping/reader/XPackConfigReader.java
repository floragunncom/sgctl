package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;

import java.io.File;
import java.io.FileNotFoundException;
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
    MigrationReport report;

    public XPackConfigReader(File elasticsearch, File user, File role, File roleMappingFile, MigrationReport report) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.roleMappingFile = roleMappingFile;
        this.ir = new IntermediateRepresentation();
        this.report = report;
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

    //region Helper Functions
    <R> R toType(Object value, Class<R> expectedType, String fileName, String path) {
        if (expectedType.isInstance(value)) {
            return expectedType.cast(value);
        }
        report.addInvalidType(fileName, path, expectedType.getTypeName(), value.getClass().getTypeName());
        return null;
    }

    static <R> List<R> readList(List<?> rawList, Function<LinkedHashMap<?, ?>, R> reader) {
        var result = new ArrayList<R>();
        for (var element : rawList) {
            if (element instanceof LinkedHashMap<?, ?> rawMap) {
                var value = reader.apply(rawMap);
                if (value == null) {
                    printErr("Item was not presented correctly.");
                    continue;
                }
                result.add(value);
            } else {
                printErr("Invalid type for item: " + element.getClass());
            }
        }
        return result;
    }

    static List<String> toStringList(Object obj, String originFile, String parameterOrigin, String key) {
        try {
            return toStringList(obj);
        } catch (IllegalArgumentException e) {
            printErr("Issue at file: " + originFile + " and parameter: " + parameterOrigin + ". Invalid type " + obj.getClass() + " for key " + key); // TODO: Add MigrationReport entry
        } catch (ClassCastException e) {
            printErr("Issue at file: " + originFile + " and parameter: " + parameterOrigin + ". Expected 'String' for item in list for key" + key + " but got " + e.getMessage()); // TODO: Add MigrationReport entry
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
                throw new ClassCastException("" + element.getClass());
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
    //endregion
}
