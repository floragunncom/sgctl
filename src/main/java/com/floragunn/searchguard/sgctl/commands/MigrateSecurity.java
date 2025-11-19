package com.floragunn.searchguard.sgctl.commands;

import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "migrate-security", description = "Converts Xpack configs to search guard config files with a given input.")
public class MigrateSecurity implements Callable<Integer> {

    @Parameters
    List<String> parameters;

    @Option(names = { "-o", "--output-dir" }, description = "Directory where to write new configuration files")
    File outputDir;

    @Override
    public Integer call() throws Exception {
        return 0;
    }
}
