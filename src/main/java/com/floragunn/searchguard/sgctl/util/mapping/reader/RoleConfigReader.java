package com.floragunn.searchguard.sgctl.util.mapping.reader;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import org.jspecify.annotations.NonNull;

import java.io.File;
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

    public RoleConfigReader(File roleFile, IntermediateRepresentation ir) throws DocumentParseException, IOException {
        this.roleFile = roleFile;
        this.ir = ir;
        this.report = MigrationReport.shared;
        readRoleFile();
    }

    private void readRoleFile() throws DocumentParseException, IOException {
        if (roleFile == null) return;
        var reader = DocReader.json().read(roleFile);

        if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
            report.addInvalidType(FILE_NAME, "origin", LinkedHashMap.class, reader);
            return;
        }
        readRoles(mapReader);
    }

    private void readRoles(LinkedHashMap<?, ?> mapReader) {
        report.addWarning(FILE_NAME, "metadata", "The key 'metadata' is ignored for migration because it has no equivalent in Search Guard");
        for (var entry : mapReader.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                report.addInvalidType(FILE_NAME, "origin", String.class, entry.getKey());
                continue;
            }
            var value = entry.getValue();

            if (value instanceof LinkedHashMap<?, ?> role) {
                readRole(role, key);
            } else {
                report.addInvalidType(FILE_NAME, "origin", LinkedHashMap.class, value);
            }
        }
    }

    private void readRole(LinkedHashMap<?, ?> roleMap, @NonNull String roleName) {
        var role = new Role(roleName);
        for (var entry : roleMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                report.addInvalidType(FILE_NAME, roleName, String.class, entry.getKey());
                continue;
            }
            var value = entry.getValue();
            var origin = roleName + "->" + key;
            switch (key) {
                case "applications":
                    if (value instanceof ArrayList<?> applicationList) {
                        role.setApplications(readList(applicationList, map->readApplication(map, roleName+"->applications"), FILE_NAME, origin));
                    } else {
                        report.addInvalidType(FILE_NAME, origin, ArrayList.class, value);
                    }
                    break;
                case "cluster":
                    role.setCluster(toStringList(value, FILE_NAME, roleName, key));
                    break;
                case "remote_cluster":
                    if (value instanceof ArrayList<?> remoteClusterList) {
                        role.setRemoteClusters(readList(remoteClusterList, map->readRemoteCluster(map, roleName+"->remoteClusters"), FILE_NAME, origin));
                    } else {
                        report.addInvalidType(FILE_NAME, origin, ArrayList.class, value);
                    }
                    break;
                case "indices":
                    if (value instanceof ArrayList<?> indices) {
                        role.setIndices(readList(indices, map -> readIndex(map, false, roleName + "->indices", Role.Index.class), FILE_NAME, origin));
                    } else {
                        report.addInvalidType(FILE_NAME, origin, ArrayList.class, value);
                    }
                    break;
                case "remote_indices":
                    if (value instanceof ArrayList<?> indices) {
                        role.setRemoteIndices(readList(indices, map -> readRemoteIndex(map, roleName + "->remote_indices"), FILE_NAME, origin));
                    } else {
                        report.addInvalidType(FILE_NAME, origin, ArrayList.class, value);
                    }
                    break;
                case "metadata":
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
                        report.addInvalidType(FILE_NAME, origin, String.class, value);
                    }
                    break;
                case "global":
                    report.addManualAction(FILE_NAME, origin, "Parameters for the key 'global' are not documented well enough for automatic migration.");
                    break;
                default:
                    report.addUnknownKey(FILE_NAME, key, origin);
                    break;
            }
        }

        ir.addRole(role);
    }

    private Role.RemoteCluster readRemoteCluster(LinkedHashMap<?, ?> map, String origin) {
        var remoteCluster = new Role.RemoteCluster();
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                report.addInvalidType(FILE_NAME, origin, String.class, entry.getKey());
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
                    report.addUnknownKey(FILE_NAME, key, origin);
                    break;
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
                report.addInvalidType(FILE_NAME, origin, String.class, entry.getKey());
                continue;
            }
            var value = entry.getValue();

            switch (key) {
                case "application":
                    if (value instanceof String applicationName) {
                        name = applicationName;
                    } else {
                        report.addInvalidType(FILE_NAME, origin, String.class, value);
                    }
                    break;
                case "privileges":
                    privileges = toStringList(value, FILE_NAME, origin, key);
                    break;
                case "resources":
                    resources = toStringList(value, FILE_NAME, origin, key);
                    break;
                default:
                    report.addUnknownKey(FILE_NAME, key, origin);
                    break;
            }
        }
        if (name == null) {
            report.addMissingParameter(FILE_NAME, "name", origin);
            return null;
        }
        if (privileges == null) {
            report.addMissingParameter(FILE_NAME, "privileges", origin);
            return null;
        }
        if (resources == null) {
            report.addMissingParameter(FILE_NAME, "resources", origin);
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
                report.addInvalidType(FILE_NAME, origin, String.class, entry.getKey());
                continue;
            }
            var value = entry.getValue();

            switch (key) {
                case "clusters":
                    if (!isRemote) {
                        report.addWarning(FILE_NAME, origin, "Found entry for key 'cluster', which is a remote-index property, in a non-remote index.");
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
                    } else {
                        report.addInvalidType(FILE_NAME, origin, LinkedHashMap.class, value);
                    }
                    break;
                case "names":
                    if (value instanceof String name) {
                        names = new ArrayList<>();
                        names.add(name);
                    } else {
                        names = toStringList(value, FILE_NAME, origin, key);
                    }
                    break;
                case "privileges":
                    privileges = toStringList(value, FILE_NAME, origin, key);
                    break;
                case "query":
                    if (value instanceof String) {
                        query = (String) value;
                    } else {
                        report.addInvalidType(FILE_NAME, origin, String.class, value);
                    }
                    break;
                case "allow_restricted_indices":
                    if (value instanceof Boolean allowRestrictedIndices) {
                        allowRestricted = allowRestrictedIndices;
                    } else {
                        report.addInvalidType(FILE_NAME, origin, Boolean.class, value);
                    }
                    break;
                default:
                    report.addUnknownKey(FILE_NAME, key, origin);
                    break;
            }
        }
        if (names == null) {
            report.addMissingParameter(FILE_NAME, "names", origin);
            return null;
        }

        if (privileges == null) {
            report.addMissingParameter(FILE_NAME, "privileges", origin);
            return null;
        }
        if (isRemote) {
            if (cluster == null) {
                report.addMissingParameter(FILE_NAME, "cluster", origin);
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
                report.addInvalidType(FILE_NAME, origin, String.class, entry.getKey());
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
                    report.addUnknownKey(FILE_NAME, key, origin);
                    break;
            }
        }
        return fieldSecurity;
    }
}
