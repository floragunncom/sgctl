package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.searchguard.sgctl.util.mapping.XPackConfigReader;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.validation.XPackConfigValidator;
import com.floragunn.searchguard.sgctl.util.mapping.validation.XPackValidationIssue;
import com.floragunn.searchguard.sgctl.util.mapping.validation.XPackValidationResult;
import com.floragunn.searchguard.sgctl.util.mapping.validation.XPackValidationSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

public class MigrateSecurity implements Callable<Integer> {

    @Parameters(description = "Path to the directory containing elasticsearch.yml and possibly other X-Pack configuration files.")
    File inputDir;

    @CommandLine.Option(names = { "-o", "--output-dir" }, description = "Directory where to write new configuration files.")
    File outputDir;

    private File elasticsearch = null;
    private File user = null;
    private File role = null;
    private File roleMapping = null;

    private static final Logger log = LoggerFactory.getLogger(MigrateSecurity.class);
    private static final String VALIDATION_ISSUE_PREFIX = " - {}";

    @Override
    public Integer call() throws Exception {
        if (!checkInputDirAndLoadConfig() || !checkOutputDir()) {
            return 1;
        }

        IntermediateRepresentation ir = readIntermediateRepresentation();

        XPackValidationResult validationResult = XPackConfigValidator.validate(ir);

        if (!validationResult.isEmpty()) {
            logValidationResult(validationResult);
        }

        if (validationResult.hasErrors()) {
            log.error("X-Pack configuration contains validation errors. Aborting migration.");
            // No writer / further processing if validation failed
            return 2;
        }

        // XR3 / XR4: generate SG YAML and write / print here

        return 0;
    }

    /**
     * Reads the X-Pack configuration and builds the intermediate representation.
     * Separated into a protected method to allow test stubbing.
     */
    protected IntermediateRepresentation readIntermediateRepresentation() {
        XPackConfigReader reader = new XPackConfigReader(elasticsearch, user, role, roleMapping);
        return reader.generateIR();
    }

    public boolean checkOutputDir() {
        if (outputDir == null) {
            log.debug("No output directory specified; skipping output directory checks.");
            return true;
        }

        if (!outputDir.exists()) {
            log.error("Output path does not exist: {}", outputDir.getAbsolutePath());
            return false;
        }
        if (!outputDir.isDirectory()) {
            log.error("Output path is not a directory: {}", outputDir.getAbsolutePath());
            return false;
        }
        if (!outputDir.canWrite()) {
            log.error("Output directory is not writeable. Check permissions: {}", outputDir.getAbsolutePath());
            return false;
        }
        return true;
    }

    public boolean checkInputDirAndLoadConfig() {
        if (inputDir == null) {
            log.error("Basic usage of migrate-security: ./sgctl.sh migrate-security <input directory>");
            return false;
        }

        if (!inputDir.exists()) {
            log.error("Input path does not exist: {}", inputDir.getAbsolutePath());
            return false;
        }

        if (!inputDir.isDirectory()) {
            log.error("Input path is not a directory: {}", inputDir.getAbsolutePath());
            return false;
        }

        if (!inputDir.canRead()) {
            log.error("Input directory is not readable. Check permissions: {}", inputDir.getAbsolutePath());
            return false;
        }

        File[] files = inputDir.listFiles();

        if (files == null) {
            log.error("Found unexpected null-value while listing files in input directory (I/O error).");
            return false;
        }

        for (File file : files) {
            String name = file.getName();

            if ("elasticsearch.yml".equals(name)) {
                elasticsearch = file;
            } else if ("user.json".equals(name)) {
                user = file;
            } else if ("role.json".equals(name)) {
                role = file;
            } else if ("role_mapping.json".equals(name)) {
                roleMapping = file;
            }
        }

        if (elasticsearch == null) {
            log.error("Required file elasticsearch.yml not found in input directory: {}", inputDir.getAbsolutePath());
            return false;
        }

        return true;
    }

    private void logValidationResult(XPackValidationResult result) {
        log.info("Validation result for X-Pack configuration:");

        for (XPackValidationIssue issue : result.getIssues()) {
            XPackValidationSeverity severity = issue.severity();

            if (severity == XPackValidationSeverity.ERROR) {
                log.error(VALIDATION_ISSUE_PREFIX, issue);
            } else if (severity == XPackValidationSeverity.WARNING) {
                log.warn(VALIDATION_ISSUE_PREFIX, issue);
            } else {
                log.info(VALIDATION_ISSUE_PREFIX, issue);
            }
        }

        log.info("");
    }
}
