package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.util.mapping.XPackConfigReader;
import com.floragunn.searchguard.sgctl.util.mapping.ir.InteremediateRepresentation;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "migrate-security", description = "Converts Xpack configs to search guard config files with a given input.")
public class MigrateSecurity implements Callable<Integer> {

    @Parameters
    File inputDir;

    @Option(names = { "-o", "--output-dir" }, description = "Directory where to write new configuration files")
    File outputDir;

    @Override
    public Integer call() throws Exception {

        File user = null;
        File role = null;
        File userMapping = null;
        InteremediateRepresentation ir = new InteremediateRepresentation();

        for (File file : inputDir.listFiles()) {
            switch(file.getName()) {
            case "user.json":
                user = file;
            case "role.json":
                role = file;
            case "user_mapping.json":
                userMapping = file;
            }
        }

        var reader = new XPackConfigReader(null, user, role, userMapping, ir);
        reader.generateIR();
        return 0;
    }
}
