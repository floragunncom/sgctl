package com.floragunn.searchguard.sgctl.util.mapping;
import com.floragunn.codova.documents.DocReader;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class XPackConfigReader {

    File elasticsearch;
    File userFile;
    File roleFile;
    File userMapping;

    public XPackConfigReader(File elasticsearch, File user, File role, File userMapping) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.userMapping = userMapping;
    }

    public void generateIR() {
        readUser();
        readRole();
        readUserMapping();
    }

    private void readUser() {
        try {
            var reader = DocReader.json().read(userFile);

            if (!(reader instanceof LinkedHashMap<?, ?> mapReader)) {
                // TODO: Add MigrationReport entry
                return;
            }
            for (var entry : mapReader.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                if (!(key instanceof String)) {
                    // TODO: Add MigrationReport entry
                    continue;
                }
                var user = (LinkedHashMap<?, ?>)value;
                if (user == null) {
                    // TODO: Add MigrationReport entry
                    printErr("Missing user entry in config file");
                    continue;
                }
                var username = user.get("username");
                if (username == null) {
                    // TODO: Add MigrationReport entry
                    printErr("Missing username for user");
                    continue;
                }
                var roles = (ArrayList<?>)user.get("roles");
                var fullName = (String)user.get("full_name");
                var email = (String)user.get("email");
                var metadata = (LinkedHashMap<?, ?>)user.get("metadata");
                metadata.forEach((k1,v1)->{
                    // TODO: Add metadata interpretation
                });
            }

//            var node = DocNode.parse(Format.JSON).from(userFile);
//            var users = node.findByJsonPath("$.*");
//
//            for (Object user : users) {
//                if (user instanceof LinkedHashMap<?,?>) {
//                    var mapUser = (LinkedHashMap<?,?>)user;
//                    print(mapUser);
//
//                    print(mapUser.get("username"));
//                    print(mapUser.get("roles"));
//                }
//            }
        } catch (Exception e) {
            printErr(e.getMessage());
        }
    }

    private void readRole() {

    }

    private void readUserMapping() {

    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
