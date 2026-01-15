package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation.RealmTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes Search Guard configuration files from the intermediate representation.
 */
public class SearchGuardConfigWriter {
    IntermediateRepresentation ir;
    Map<String, Document<?>> writers;

    /**
     * Creates a Search Guard configuration writer and initializes all sub-writers
     * based on the provided intermediate representation.
     * @param ir the intermediate representation produced by the migration process
     */
    public SearchGuardConfigWriter(IntermediateRepresentation ir) {
        writers = new HashMap<>();
        var sgTranslator = new SGAuthcWriter(ir.getElasticSearchYml());
        var sgAuthc = sgTranslator.getConfig();
        var sgFrontendAuthc = sgTranslator.getFrontEndConfig();
        writers.put(sgAuthc.fileName, sgAuthc);
        writers.put(sgFrontendAuthc.fileName, sgFrontendAuthc);
        writers.put(ElasticSearchConfigWriter.FILE_NAME, new ElasticSearchConfigWriter(ir.getElasticSearchYml()));
        writers.put(UserConfigWriter.FILE_NAME, new UserConfigWriter(ir));
        var actionGroupConfig = new ActionGroupConfigWriter();
        writers.put(ActionGroupConfigWriter.FILE_NAME, actionGroupConfig);
        writers.put(RoleConfigWriter.FILE_NAME, new RoleConfigWriter(ir, sgAuthc, actionGroupConfig));
        writers.put(RoleMappingWriter.FILE_NAME, new RoleMappingWriter(ir));
        this.ir = ir;
    }

    /**
     * Writes content to a file using a specific writer.
     *
     * @param directory output dir
     * @param fileName file name
     * @param content content to write
     * @throws IOException if write fails
     */
    private void writeFile(File directory, String fileName, String content) throws IOException {
        Files.writeString(new File(directory.getPath(), fileName).toPath(), content);
    }

    /**
     * Prints a file with a header
     * @param fileName File Header
     * @param content Content of the file
     */
    private void printFile(String fileName, String content) {
        printHeader(fileName);
        print(content);
        printFooter();
    }

    /**
     * Writes all generated Search Guard configuration files to the given directory or prints them if no directory was specified.
     * <p>
     * Each configuration section is serialized as YAML and written to its
     * corresponding file name (e.g. roles, users, action groups, authc).
     *
     * @param directory the target directory for the generated configuration files
     * @throws IOException if writing any file fails
     */
    public void outputContent(File directory) throws IOException {
        final var docWriter = DocWriter.yaml();
        for (Map.Entry<String, Document<?>> writer : writers.entrySet()) {
            String fileHeader = writer.getKey();
            String content = docWriter.writeAsString(writer.getValue());

            if (directory == null || !directory.exists()) {
                printFile(fileHeader, content);
            } else {
                writeFile(directory, fileHeader, content);
            }
        }
    }

    static private void printHeader(String filename) {
        print("\n----------------------------- " + filename + " --------------------------------------");
    }

    static private void printFooter() {
        print("\n---------------------------------------------------------------------------------\n");
    }

    static void print(Object line) {
        System.out.println(line);
    }
}
