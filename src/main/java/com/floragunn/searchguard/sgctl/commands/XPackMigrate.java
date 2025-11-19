package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.UnexpectedDocumentStructureException;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.config.xpack.Role;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

@CommandLine.Command(name = "migrate-security", description = "Converts X-Pack configs to Search Guard configs")
public class XPackMigrate implements Callable<Integer> {

    @CommandLine.Option(names = { "-i", "--input-dir" }, description = "Location of the old x-pack configuration files", required = true)
    Path inputDir;

    @CommandLine.Option(names = { "-o", "--output-dir" }, description = "Directory where to write new sg configuration files", required = true)
    Path outputDir;

    private static final Map<String, Function<Map<String, Object>, Record>> configParsers = Map.of(
            // TODO: Add parsing functions here <filename>,Record::parse
    );

    public Integer call() throws Exception {
        if (!Files.isDirectory(inputDir)) {
            System.err.println("Error: Input is not a directory: " + inputDir.toAbsolutePath());
            return 1;
        }
        System.out.println("Welcome to the Search Guard X-pack security migration tool.\n\n");
        try {
            final var configs = parseConfigs();
            System.out.println(configs);
            // TODO: More

        } catch (SgctlException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
        return 0;
    }

    private Map<String, Record> parseConfigs()
            throws SgctlException, IOException, DocumentParseException, UnexpectedDocumentStructureException, IllegalArgumentException {
        final Map<String, Record> configs = new HashMap<>();
        for (final var entry : configParsers.entrySet()) {
            final var configFileName = entry.getKey();
            final var parserFunction = entry.getValue();
            // Ensure valid path
            final var configPath = inputDir.resolve(configFileName);
            if (!Files.isReadable(configPath)) {
                continue;
            }
            if (Files.isDirectory(configPath)) {
                throw new SgctlException("Config is a directory, but should be a file: " + configFileName);
            }
            // Read to Object
            final Map<String, Object> config;
            if (configFileName.endsWith(".yaml") || configFileName.endsWith(".yml")) {
                config = DocReader.yaml().readObject(configPath.toFile());
            } else if (configFileName.endsWith(".json")) {
                config = DocReader.json().readObject(configPath.toFile());
            } else {
                throw new SgctlException("Invalid config file extension: " + configFileName);
            }

            // Parse config
            final Record parsed = parserFunction.apply(config);
            configs.put(configFileName, parsed);
        }
        return configs;
    }

}
