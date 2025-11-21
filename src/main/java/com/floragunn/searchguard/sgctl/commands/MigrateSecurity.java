package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.util.mapping.XpackConfigReader;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
//names of files: user.json, role.json, role_mapping.json
@Command(name = "migrate-security", description = "Converts Xpack configs to search guard config files with a given input.")
public class MigrateSecurity implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the directory containing elasticsearch.yml.")
    File inputDir;

    @Option(names = { "-o", "--output-dir" }, description = "Directory where to write new configuration files")
    File outputDir;

    File elasticsearch = null;
    File user = null;
    File role = null;
    File roleMapping = null;

    @Override
    public Integer call() throws Exception {

        String path = (inputDir != null) ? inputDir.getAbsolutePath() : "NULL";

        if (inputDir == null) {
            throw new SgctlException("Basic Usage of migrate-security: ./sgctl.sh migrate-security <Input Directory> ");
        }
        if (!inputDir.exists()) {
            throw new SgctlException("Input path does not exist: " + inputDir.getAbsolutePath());
        }

        if (!inputDir.isDirectory()) {
            throw new SgctlException("Input path is not a directory: " + inputDir.getAbsolutePath());
        }

        if (!inputDir.canRead()) {
            throw new SgctlException("Input directory is not readable. Check permissions: " + inputDir.getAbsolutePath());
        }

        var files = inputDir.listFiles();

        if (files == null) {
            throw new SgctlException("Found unexpected null-value while listing files in input directory (I/O error).");
        }

        for (File file : files) {
            switch (file.getName()) {
                case "elasticsearch.yml":
                    elasticsearch= file;
                case "user.json":
                    user = file;
                case "role.json":
                    role = file;
                case "role_mapping.json":
                    roleMapping = file;
            }
        }

        if (elasticsearch == null) {
            System.err.println("Required file elasticsearch.yml not found.");
            return 1;
        }

        return 0;
    }
}