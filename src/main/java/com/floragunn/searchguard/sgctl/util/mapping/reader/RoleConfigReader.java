package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.floragunn.searchguard.sgctl.util.mapping.reader.XPackConfigReader.readList;
import static com.floragunn.searchguard.sgctl.util.mapping.reader.XPackConfigReader.toStringList;

public class RoleConfigReader {
    File roleFile;
    IntermediateRepresentation ir;
    MigrationReport report;

    static final String FILE_NAME = "role.json";

    public RoleConfigReader(File roleFile, IntermediateRepresentation ir, MigrationReport report) {
        this.roleFile = roleFile;
        this.ir = ir;
        this.report = report;
        readRoleFile();
    }

    private void readRoleFile() {
        if (roleFile == null) return;
        try {
            var reader = DocReader.json().read(roleFile);

            if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
                // TODO: Add MigrationReport entry
                return;
            }
            readRoles(mapReader);
        } catch (DocumentParseException e) {
            printErr("Error while parsing file."); // TODO: Add MigrationReport entry
        } catch (FileNotFoundException e) {
            printErr("File not found."); // TODO: Add MigrationReport entry
        } catch (IOException e) {
            printErr("Unexpected Error while accessing file."); // TODO: Add MigrationReport entry
        }
    }

    private void readRoles(LinkedHashMap<?, ?> mapReader) {
        for (var entry : mapReader.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            if (value instanceof LinkedHashMap<?, ?> role) {
                readRole(role, key);
            } else {
                printErr("Invalid value for role: " + value); // TODO: Add MigrationReport entry
            }
        }
    }

    private void readRole(LinkedHashMap<?, ?> roleMap, @NonNull String roleName) {
        var role = new Role(roleName);
        for (var entry : roleMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            switch (key) {
                case "applications":
                    if (value instanceof ArrayList<?> applicationList) {
                        role.setApplications(readList(applicationList, map->readApplication(map, roleName+"->applications")));
                    } else {
                        printErr("Invalid type for applications: " + value.getClass()); // TODO: Add MigrationReport entry
                    }
                    break;
                case "cluster":
                    role.setCluster(toStringList(value, FILE_NAME, roleName, key));
                    break;
                case "remote_cluster":
                    if (value instanceof ArrayList<?> remoteClusterList) {
                        role.setRemoteClusters(readList(remoteClusterList, map->readRemoteCluster(map, roleName+"->remoteClusters")));
                    }
                    break;
                case "indices":
                    if (value instanceof ArrayList<?> indices) {
                        role.setIndices(readList(indices, map -> readIndex(map, false, roleName + "->indices", Role.Index.class)));
                    } else {
                        printErr("Invalid type for indices: " + value.getClass()); // TODO: Add MigrationReport entry
                    }
                    break;
                case "remote_indices":
                    if (value instanceof ArrayList<?> indices) {
                        role.setRemoteIndices(readList(indices, map -> readRemoteIndex(map, roleName + "->remote_indices")));
                    } else {
                        printErr("Invalid type for remote_indices: " + value.getClass()); // TODO: Add MigrationReport entry
                    }
                    break;
                case "metadata":
                    printErr("Metadata is ignored for migration."); // TODO: Add MigrationReport entry
                    break;
                case "transient_metadata":
                    // TODO: Add support for transient metadata interpretation
                    break;
                case "run_as":
                    role.setRunAs(toStringList(value, FILE_NAME, roleName, "runAs"));
                    break;
                case "description":
                    if (value instanceof String description) {
                        role.setDescription(description);
                    } else {
                        printErr("Invalid type for description: " + value.getClass()); // TODO: Add MigrationReport entry
                    }
                    break;
                default:
                    printErr("Unknown key: " + key); // TODO: Add MigrationReport entry
            }
        }

        ir.addRole(role);
    }

    private Role.RemoteCluster readRemoteCluster(LinkedHashMap<?, ?> map, String origin) {
        var remoteCluster = new Role.RemoteCluster();
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();
            switch (key) {
                case "clusters":
                    remoteCluster.setClusters(toStringList(value, FILE_NAME, origin, key));
                    break;
                case "privileges":
                    remoteCluster.setPrivileges(toStringList(value, FILE_NAME, origin, key));
                    break;
                default:
                    printErr("Unknown key: " + key); // TODO: Add MigrationReport entry
            }
        }

        return remoteCluster;
    }

    private Role.Application readApplication(LinkedHashMap<?, ?> applicationMap, String origin) {
        String name = null;
        List<String> privileges = null;
        List<String> resources = null;
        for (var entry : applicationMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            switch (key) {
                case "application":
                    if (value instanceof String applicationName) {
                        name = applicationName;
                    } else {
                        printErr("Invalid value type for application: " + value.getClass()); // TODO: Add MigrationReport entry
                        return null;
                    }
                    break;
                case "privileges":
                    privileges = toStringList(value, FILE_NAME, origin, key);
                    break;
                case "resources":
                    resources = toStringList(value, FILE_NAME, origin, key);
                    break;
                default:
                    printErr("Unknown key: " + key); // TODO: Add MigrationReport entry
                    break;
            }
        }
        if (name == null || privileges == null || resources == null) {
            printErr("Application missing required parameter."); // TODO: Add MigrationReport entry
            return null;
        }
        return new Role.Application(name, privileges, resources);
    }

    private Role.RemoteIndex readRemoteIndex(LinkedHashMap<?, ?> indexMap, String origin) {
        return readIndex(indexMap, true, origin, Role.RemoteIndex.class);
    }

    private <T extends Role.Index> T readIndex(LinkedHashMap<?, ?> indexMap, boolean isRemote, String origin, Class<T> type) {
        List<String> cluster = null;
        List<String> names = null;
        List<String> privileges = null;
        Role.FieldSecurity fieldSecurity = null;
        String query = null;
        Boolean allowRestricted = null;
        for (var entry : indexMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();

            // TODO: Add query key
            switch (key) {
                case "clusters":
                    if (!isRemote) {
                        printErr("Found unexpected key clusters. In non remote index."); // TODO: Add MigrationReport entry
                        break;
                    }
                    if (value instanceof String clusterName) {
                        cluster = new ArrayList<>();
                        cluster.add(clusterName);
                        break;
                    }
                    cluster = toStringList(value, FILE_NAME, origin, key);
                    break;
                case "field_security":
                    if (value instanceof LinkedHashMap<?, ?> fieldSecurityMap) {
                        fieldSecurity = readFieldSecurity(fieldSecurityMap, origin);
                        break;
                    }
                    // TODO: Add MigrationReport entry
                    break;
                case "names":
                    if (value instanceof String name) {
                        names = new ArrayList<>();
                        names.add(name);
                        break;
                    }
                    names = toStringList(value, FILE_NAME, origin, key);
                    break;
                case "privileges":
                    privileges = toStringList(value, FILE_NAME, origin, key);
                    break;
                case "query":
                    if (value instanceof String) {
                        query = (String) value;
                        break;
                    }
                    printErr("Unsupported format for query."); // TODO: possibly add support for other formats see: https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-security-put-role#operation-security-put-role-body-application-json-indices-query
                    break;
                case "allow_restricted_indices":
                    if (value instanceof Boolean allowRestrictedIndices) {
                        allowRestricted = allowRestrictedIndices;
                        break;
                    }
                    printErr("Invalid type for allow_restricted_indices: " + key); // TODO: Add MigrationReport entry
                    break;
                default:
                    printErr("Unknown key: " + key); // TODO: Add MigrationReport entry
                    break;
            }
        }
        if (names == null) {
            printErr("Index missing required parameter 'names'"); // TODO: Add MigrationReport entry
            return null;
        }

        if (privileges == null) {
            printErr("Index missing required parameter 'names'"); // TODO: Add MigrationReport entry
            return null;
        }
        if (isRemote) {
            if (cluster == null) {
                printErr("Index missing required parameter 'clusters'"); // TODO: Add MigrationReport entry
                return null;
            }
            return type.cast(new Role.RemoteIndex(cluster, names, privileges, fieldSecurity, query, allowRestricted));
        }
        return type.cast(new Role.Index(names, privileges, fieldSecurity, query, allowRestricted));
    }

    private Role.FieldSecurity readFieldSecurity(LinkedHashMap<?, ?> fieldMap, String origin) {
        origin = origin + "->field_security";
        var fieldSecurity = new Role.FieldSecurity();
        for (var entry : fieldMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                printErr("Invalid type " + entry.getKey().getClass() + " for key."); // TODO: Add MigrationReport entry
                continue;
            }
            var value = entry.getValue();
            switch (key) {
                case "except":
                    fieldSecurity.setExcept(toStringList(value, FILE_NAME, origin, key));
                    break;
                case "grant":
                    fieldSecurity.setGrant(toStringList(value, FILE_NAME, origin, key));
                    break;
                default:
                    // TODO: Add MigrationReport entry
                    break;
            }
        }
        return fieldSecurity;
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
