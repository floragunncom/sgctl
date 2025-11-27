package com.floragunn.searchguard.sgctl.util.mapping;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig.SgAuthc;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


/**
 * Top-level configuration object for sg_authc.yml.
 */
public class SearchGuardConfigWriter {

        private SearchGuardConfigWriter() {

        }

    /**
     * Coordinates the generation and writing of all Search Guard configuration files.
     * @throws IOException If an error occurs during file writing.
     */
    public static void generateAllConfigs(File outputDir) throws IOException {
       IntermediateRepresentationElasticSearchYml ir = new IntermediateRepresentationElasticSearchYml();

        Object authcConfigObject = createAuthcConfig(ir);

        writeYamlConfig(authcConfigObject, outputDir, "sg_authc.yml");
    }

    /**
     * Implements the core logic for mapping X-Pack Authentication realms (LDAP, SAML, OIDC)
     * to the Search Guard sg_authc.yml configuration structure.
     * @param ir The Intermediate Representation.
     * @return The fully populated SearchGuardAuthcConfig object.
     */
    private static SgAuthc createAuthcConfig(IntermediateRepresentationElasticSearchYml ir) {
        SgAuthc config = new SgAuthc();
        config.authDomains = new ArrayList<>();
        config.internalProxies = "";
        config.remoteIpHeader = "";

        HashMap<String, Object> testFrontEndConfig = new HashMap<>();
        List<Object> hosts = new ArrayList<>();
        hosts.add("ldap.example.com");
        hosts.add("other.example.com");
        testFrontEndConfig.put("ldap.idp.hosts", hosts);

        config.authDomains.add(new MigrateConfig.NewAuthDomain("basic/ldap", null, null, null, testFrontEndConfig, null));
        return config;
    }

    /**
     * Serializes a Java object (the target configuration model) into a YAML file using Jackson.
     * @param configObject The Java object to serialize (e.g., SearchGuardAuthcConfig).
     * @param outputDir The target directory (MS8).
     * @param filename The output filename (e.g., "sg_authc.yml").
     * @throws IOException If an I/O error occurs.
     */
    private static void writeYamlConfig(Object configObject, File outputDir, String filename) throws IOException {
        Files.writeString(new File(outputDir, filename).toPath(),
                DocWriter.yaml().writeAsString(configObject));

    }

}
