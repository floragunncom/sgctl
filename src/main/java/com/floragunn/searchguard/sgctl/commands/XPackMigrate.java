package com.floragunn.searchguard.sgctl.commands;

import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "migrate-xsecurity", description = "Converts X-Pack configs to Search Guard configs")
public class XPackMigrate {

    @CommandLine.Option(names = { "-i", "--input-dir" }, description = "Location of the old x-pack configuration files")
    File inputDir;

    @CommandLine.Option(names = { "-o", "--output-dir" }, description = "Directory where to write new sg configuration files")
    File outputDir;

    public Integer call() throws Exception {
        return 1;
    }

}
