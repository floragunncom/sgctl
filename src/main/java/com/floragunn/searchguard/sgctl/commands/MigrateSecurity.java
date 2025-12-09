package com.floragunn.searchguard.sgctl.commands;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.searchguard.sgctl.util.mapping.writer.ElasticSearchConfigWriter;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.reader.ElasticsearchYamlReader;
import com.floragunn.searchguard.sgctl.util.mapping.writer.SearchGuardConfigWriter;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;

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
    private static final Logger log = LoggerFactory.getLogger(MigrateSecurity.class);

    @Override
    public Integer call() throws Exception {
        if(!checkInputDirAndLoadConfig() || !checkOutputDir()){
            return 1;
        }

        IntermediateRepresentationElasticSearchYml irElasticSearchYml = new IntermediateRepresentationElasticSearchYml();
        new ElasticsearchYamlReader(elasticsearch, irElasticSearchYml);

        IntermediateRepresentation ir = new IntermediateRepresentation();
        SearchGuardConfigWriter sgcw = new SearchGuardConfigWriter(irElasticSearchYml, ir);

        ElasticSearchConfigWriter.writeElasticSearchYml(irElasticSearchYml, outputDir);
        writeYamlConfig(sgcw.getSg_frontend_authc(), outputDir, "sg_frontend_authc.yml");
        MigrationReport.shared.printReport();
        return 0;
    }

    public boolean checkOutputDir() {
        if (outputDir == null) {
            log.error("Basic Usage of migrate-security: ./sgctl.sh migrate-security <Input Directory> -o <Output Directory>");
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
            log.error("Basic Usage of migrate-security: ./sgctl.sh migrate-security <Input Directory> ");
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

        var files = inputDir.listFiles();

        if (files == null) {
            log.error("Found unexpected null-value while listing files in input directory (I/O error).");
            return false;
        }else{
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
        }
        if (elasticsearch == null) {
            log.error("Required file elasticsearch.yml not found.");
            return false;
        }
        return true;
    }

    /**
     * Serializes a Java object into a YAML file.
     *
     * @param configObject The object to serialize.
     * @param outputDir    Target directory.
     * @param filename     Name of the output YAML file.
     * @throws IOException If writing fails.
     */
    private void writeYamlConfig(Object configObject, File outputDir, String filename) throws IOException {
        File outputFile = new File(outputDir, filename);
        Files.writeString(outputFile.toPath(), DocWriter.yaml().writeAsString(configObject));
    }
}
