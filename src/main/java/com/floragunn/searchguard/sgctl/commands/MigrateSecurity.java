package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.util.mapping.XPackConfigReader;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;
//names of files: user.json, role.json, role_mapping.json
@Command(name = "migrate-security",mixinStandardHelpOptions = true, description = "Converts X-Pack configuration to Search Guard configuration files with a given input.")
public class MigrateSecurity implements Callable<Integer> {

    @Parameters(description = "Path to the directory containing elasticsearch.yml and possibly other X-Pack configuration files.")
    File inputDir;

    @Option(names = { "-o", "--output-dir" }, description = "Directory where to write new configuration files.")
    File outputDir;

    private File elasticsearch = null;
    private File user = null;
    private File role = null;
    private File roleMapping = null;

    @Override
    public Integer call() throws Exception {
        if(!checkInputDirAndLoadConfig() || !checkOutputDir()){
            return 1;
        }
        var reader = new XPackConfigReader(elasticsearch, user, role, roleMapping);
        return 0;
    }

    public boolean checkOutputDir() {
        if(outputDir == null) {
            return true;
        }
        if(!outputDir.exists()) {
            return false;
        }
        if(!outputDir.isDirectory()) {
            return false;
        }
        if(!outputDir.canWrite()) {
            return false;
        }
        return true;
    }

    public boolean checkInputDirAndLoadConfig() {

        if (inputDir == null) {
            System.err.println("Basic Usage of migrate-security: ./sgctl.sh migrate-security <Input Directory> ");
            return false;
        }

        if (!inputDir.exists()) {
            System.err.println("Input path does not exist: " + inputDir.getAbsolutePath());
            return false;
        }

        if (!inputDir.isDirectory()) {
            System.err.println("Input path is not a directory: " + inputDir.getAbsolutePath());
            return false;
        }

        if (!inputDir.canRead()) {
            System.err.println("Input directory is not readable. Check permissions: " + inputDir.getAbsolutePath());
            return false;
        }

        var files = inputDir.listFiles();

        if (files == null) {
            System.err.println("Found unexpected null-value while listing files in input directory (I/O error).");
            return false;
        }else{
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
        }
        if (elasticsearch == null) {
            System.err.println("Required file elasticsearch.yml not found.");
            return false;
        }
        return true;
    }
}