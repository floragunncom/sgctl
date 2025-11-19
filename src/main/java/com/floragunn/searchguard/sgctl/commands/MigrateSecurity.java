package com.floragunn.searchguard.sgctl.commands;

import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "migrate-security", description = "Converts Xpack config files to search guard config files, also prints a report based on the conversions success.")
public class MigrateSecurity implements Callable<Integer> {

    @Parameters(description = "Directory with all important Files, such as elasticsearch.yml")
    File inputPath;

    @Option(names = { "-o", "--output-dir" }, description = "Directory where to write new configuration files")
    File outputDir;

    @Override
    public Integer call() throws Exception {
        if (inputPath == null || !inputPath.exists()) {
            System.err.println(
                    "You must specify a valid directory to the xpack configuration files elasticsearch.yml and security.index on the command line");
            return 1;
        }

        System.out.println("Completed Command");
        return 0;
    }
}
