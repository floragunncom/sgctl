/*
 * Copyright 2021 floragunn GmbH
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

package com.floragunn.searchguard.sgctl.commands.user;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

import com.floragunn.codova.documents.DocWriter;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "add-user-local", description = "Adds a new user to a local sg_internal_users.yml file")
public class AddUserLocal implements Callable<Integer> {

    @Parameters(index = "0", arity = "1", description = "User name")
    private String userName;

    @Option(names = { "-r", "--sg-roles" }, split = ",")
    private List<String> sgRoles;

    @Option(names = { "--backend-roles" }, split = ",")
    private List<String> backendRoles;

    @Option(names = { "-a", "--attributes" }, split = ",")
    private Map<String, Object> attributes;

    @Option(names = { "--password" }, arity = "0..1", description = "Passphrase", interactive = true, required = true)
    char[] password;

    @Option(names = { "-o",
            "--output" }, arity = "0..1", description = "File or directory to write configuration to. If not specified, the configuration is written to STDOUT.")
    File output;

    @Override
    public Integer call() {
        try {

            Map<String, Object> newUserData = new LinkedHashMap<>();

            if (sgRoles != null) {
                newUserData.put("search_guard_roles", sgRoles);
            }

            if (backendRoles != null) {
                newUserData.put("backend_roles", backendRoles);
            }

            if (attributes != null) {
                newUserData.put("attributes", attributes);
            }

            if (password != null) {
                newUserData.put("hash", hash(password));
                Arrays.fill(password, (char) 0);
            }

            String userYaml = DocWriter.yaml().writeAsString(ImmutableMap.of(userName, newUserData));

            if (output == null) {
                System.out.println(userYaml);
            } else {
                if (output.isDirectory()) {
                    output = new File(output, "sg_internal_users.yml");
                } 
                
                if (output.exists()) {
                    String existing = Files.asCharSource(output, Charsets.UTF_8).read();
                    
                    if (!existing.endsWith("\n")) {
                        existing += "\n";
                    }
                    
                    System.out.println("Appending to " + output);
                    
                    if (userYaml.startsWith("---")) {
                        userYaml = userYaml.substring(3).trim();
                    }
                    
                    Files.asCharSink(output, Charsets.UTF_8).write(existing + userYaml);
                } else {
                    System.out.println("Wrting to " + output);
                    
                    Files.asCharSink(output, Charsets.UTF_8).write(userYaml);
                }
            }

            return 0;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return 1;
        } 
    }

    private static String hash(char[] clearTextPassword) {
        final byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        final String hash = OpenBSDBCrypt.generate((Objects.requireNonNull(clearTextPassword)), salt, 12);
        Arrays.fill(salt, (byte) 0);
        Arrays.fill(clearTextPassword, '\0');
        return hash;
    }

}
