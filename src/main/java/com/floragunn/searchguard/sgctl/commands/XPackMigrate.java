package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.SgctlException;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "migrate-security", description = "Converts X-Pack configs to Search Guard configs")
public class XPackMigrate implements Callable<Integer> {

    @CommandLine.Option(names = { "-i", "--input-dir" }, description = "Location of the old x-pack configuration files", required = true)
    Path inputDir;

    @CommandLine.Option(names = { "-o", "--output-dir" }, description = "Directory where to write new sg configuration files", required = true)
    Path outputDir;

    private static final List<String> configNames = List.of("elasticsearch.yml", "roles.json", "users.json");

    public Integer call() throws Exception {
        if (!Files.isDirectory(inputDir)) {
            System.err.println("Error: Input is not a directory: " + inputDir.toAbsolutePath());
            return 1;
        }
        System.out.println("Welcome to the Search Guard X-pack security migration tool.\n\n");
        try {
            final var configStrings = readConfigStrings();
            System.out.println(configStrings);
            // TODO: More


        } catch (SgctlException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
        return 0;
    }

    private Map<String, String> readConfigStrings() throws SgctlException, IOException, IllegalArgumentException {
        final Map<String, String> configs = new HashMap<>();
        for (String configFileName : configNames) {
            final var configPath = inputDir.resolve(configFileName);
            if (Files.isReadable(configPath)) {
                if (Files.isDirectory(configPath)) {
                    throw new SgctlException("Config is a directory, but should be a file: " + configFileName);
                }

                final String content = Files.readString(configPath);
                configs.put(configFileName, content);
            }
        }
        return configs;
    }

}
