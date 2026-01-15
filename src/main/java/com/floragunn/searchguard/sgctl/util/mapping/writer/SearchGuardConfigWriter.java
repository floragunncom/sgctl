package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation.RealmTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Writes Search Guard configuration files from the intermediate representation.
 */
public class SearchGuardConfigWriter {
    SGAuthcWriter.SgAuthc sgAuthc;
    SGAuthcWriter.SgAuthc sgFrontendAuthc;
    ElasticSearchConfigWriter elasticSearchConfig;
    UserConfigWriter userConfig;
    ActionGroupConfigWriter actionGroupConfig;
    RoleConfigWriter roleConfig;
    RoleMappingWriter mappingWriter;
    IntermediateRepresentation ir;

    /**
     * Creates a Search Guard configuration writer and initializes all sub-writers
     * based on the provided intermediate representation.
     * @param ir the intermediate representation produced by the migration process
     */
    public SearchGuardConfigWriter(IntermediateRepresentation ir) {
        var sgTranslator = new SGAuthcWriter(ir.getElasticSearchYml());
        sgAuthc = sgTranslator.getConfig();
        sgFrontendAuthc = sgTranslator.getFrontEndConfig();
        elasticSearchConfig = new ElasticSearchConfigWriter(ir.getElasticSearchYml());
        if (ir.getUsers().isEmpty()) {
            userConfig = null;
        } else {
            userConfig = new UserConfigWriter(ir);
        }
        if (ir.getRoles().isEmpty()) {
            actionGroupConfig = null;
        } else {
            actionGroupConfig = new ActionGroupConfigWriter();
            roleConfig = new RoleConfigWriter(ir, sgAuthc, actionGroupConfig);
        }
        if (ir.getRoleMappings().isEmpty()) {
            mappingWriter = null;
        } else {
            mappingWriter = new RoleMappingWriter(ir);
        }
        this.ir = ir;
    }

    /**
     * Writes content to a file using a specific writer.
     *
     * @param directory output dir
     * @param fileName file name
     * @param content content to write
     * @param writer writer that writes the content
     * @throws IOException if write fails
     */
    private void writeFile(File directory, String fileName, Document<?> content, DocWriter writer) throws IOException {
        if (content == null) return;
        Files.write(new File(directory.getPath(), fileName).toPath(), writer.writeAsString(content).getBytes());
    }

    /**
     * Prints a file with a header
     * @param fileName File Header
     * @param content Content of the file
     * @param writer writer to translate content
     */
    private void printFile(String fileName, Document<?> content, DocWriter writer) {
        if (content == null) return;
        printHeader(fileName);
        print(writer.writeAsString(content));
        printFooter();
    }

    /**
     * Writes all generated Search Guard configuration files to the given directory.
     * <p>
     * Each configuration section is serialized as YAML and written to its
     * corresponding file name (e.g. roles, users, action groups, authc).
     *
     * @param directory the target directory for the generated configuration files
     * @throws IOException if writing any file fails
     */
    public void outputContent(File directory) throws IOException {
        if (directory == null) {
            outputContent();
            return;
        }
        final var writer = DocWriter.yaml();
        writeFile(directory, RealmTranslator.SG_AUTHC_FILE_NAME, sgAuthc, writer);
        writeFile(directory, RealmTranslator.SG_FRONTEND_AUTHC_FILE_NAME, sgFrontendAuthc, writer);
        writeFile(directory, ElasticSearchConfigWriter.FILE_NAME, elasticSearchConfig, writer);
        writeFile(directory, UserConfigWriter.FILE_NAME, userConfig, writer);
        writeFile(directory, RoleConfigWriter.FILE_NAME, roleConfig, writer);
        writeFile(directory, ActionGroupConfigWriter.FILE_NAME, actionGroupConfig, writer);
        writeFile(directory, RoleMappingWriter.FILE_NAME, mappingWriter, writer);
    }

    /**
     * Prints all generated Search Guard configuration files to standard output.
     */
    public void outputContent() {
        var writer = DocWriter.yaml();

        printFile(RealmTranslator.SG_AUTHC_FILE_NAME, sgAuthc, writer);

        printFile(RealmTranslator.SG_FRONTEND_AUTHC_FILE_NAME, sgFrontendAuthc, writer);

        printFile(ElasticSearchConfigWriter.FILE_NAME, elasticSearchConfig, writer);

        printFile(UserConfigWriter.FILE_NAME, userConfig, writer);

        printFile(RoleConfigWriter.FILE_NAME, roleConfig, writer);

        printFile(ActionGroupConfigWriter.FILE_NAME, actionGroupConfig, writer);

        printFile(RoleMappingWriter.FILE_NAME, mappingWriter, writer);
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
