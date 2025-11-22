package com.floragunn.searchguard.sgctl.util.mapping;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.searchguard.sgctl.util.mapping.ir.InteremediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.User;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class XPackConfigReader {

    File elasticsearch;
    File userFile;
    File roleFile;
    File userMapping;
    InteremediateRepresentation ir;

    public XPackConfigReader(File elasticsearch, File user, File role, File userMapping, InteremediateRepresentation ir) {
        this.elasticsearch = elasticsearch;
        this.userFile = user;
        this.roleFile = role;
        this.userMapping = userMapping;
        this.ir = ir;
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
                var username = (String) user.get("username");
                if (username == null) {
                    // TODO: Add MigrationReport entry
                    printErr("Missing username for user");
                    continue;
                }
                var roles = (ArrayList<String>)user.get("roles");
                // TODO: Compare roles with role file
                var fullName = (String) user.get("full_name");
                var email = (String) user.get("email");
                var metadata = (LinkedHashMap<?, ?>)user.get("metadata");
                metadata.forEach((k1,v1)->{
                    // TODO: Add metadata interpretation
                });
                ir.addUser(new User(username, roles, fullName, email));
            }

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
