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

        SgAuthc authcConfig = createAuthcConfig(ir);

        writeYamlConfig(authcConfig, outputDir, "sg_authc.yml");
    }

    /**
     * Maps LDAP configuration from intermediate representation to Search Guard's sg_authc.yml format.
     *
     * @param ir The intermediate representation.
     * @return Populated SgAuthc object.
     */
    private static SgAuthc createAuthcConfig(IntermediateRepresentationElasticSearchYml ir) {
        SgAuthc config = new SgAuthc();
        config.authDomains = new ArrayList<>();
        config.internalProxies = "";
        config.remoteIpHeader = "";

        config.authDomains.add(createLdapDomain());

        config.authDomains.add(createOidcDomain());

        return config;
    }

    /**
     * Erstellt die LDAP-Auth-Domain für sg_authc.yml
     */
    private static MigrateConfig.NewAuthDomain createLdapDomain() {
        Map<String, Object> ldapConfig = new HashMap<>();
        List<String> ldapHosts = Arrays.asList("ldap.example.com", "other.example.com");
        ldapConfig.put("ldap.idp.hosts", ldapHosts);

        return new MigrateConfig.NewAuthDomain(
                "basic/ldap",
                null,
                null,
                null,
                ldapConfig,
                null
        );
    }

    /**
     * Erstellt die OIDC-Auth-Domain für sg_authc.yml
     */
    private static MigrateConfig.NewAuthDomain createOidcDomain() {
        Map<String, Object> oidcConfig = new HashMap<>();


        return new MigrateConfig.NewAuthDomain(
                "oidc",
                null,
                null,
                null,
                oidcConfig,
                null
        );
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
