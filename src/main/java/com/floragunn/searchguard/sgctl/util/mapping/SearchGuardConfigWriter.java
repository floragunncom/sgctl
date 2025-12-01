package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.writer.SGAuthcTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Top-level configuration writer for Search Guard.
 * Generates sg_authc.yml with LDAP, SAML, OIDC realms mapped.
 */
public class SearchGuardConfigWriter {

    private SearchGuardConfigWriter() { }

    /**
     * Generates all Search Guard configuration files into the given directory.
     *
     * @param outputDir The directory where config files will be written.
     * @throws IOException If writing fails.
     */
    public static void generateAllConfigs(File outputDir) throws IOException {
        IntermediateRepresentationElasticSearchYml ir = new IntermediateRepresentationElasticSearchYml();

        MigrateConfig.SgAuthc authcConfig = SGAuthcTranslator.createAuthcConfig(ir);

        writeYamlConfig(authcConfig, outputDir, "sg_authc.yml");
    }


    /**
     * Serializes a Java object into a YAML file.
     *
     * @param configObject The object to serialize.
     * @param outputDir    Target directory.
     * @param filename     Name of the output YAML file.
     * @throws IOException If writing fails.
     */
    private static void writeYamlConfig(Object configObject, File outputDir, String filename) throws IOException {
        File outputFile = new File(outputDir, filename);
        Files.writeString(outputFile.toPath(), DocWriter.yaml().writeAsString(configObject));
    }
}
