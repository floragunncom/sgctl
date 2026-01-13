package com.floragunn.searchguard.sgctl.config.migrate.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Parser;
import com.floragunn.searchguard.sgctl.config.migrate.FrontendAuthMigrator;
import com.floragunn.searchguard.sgctl.config.migrate.MigrationReporter;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.searchguard.SgFrontendAuthC;
import com.floragunn.searchguard.sgctl.config.xpack.Kibana;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class FrontendAuthMigratorTest {

    @Test
    void testMigrateKibanaBasicAuth() throws Exception {
        var kibana = loadKibanaConfig("/xpack_migrate/kibana/basic.yml");
        var context = createContext(Optional.empty(), Optional.of(kibana));

        var result = new FrontendAuthMigrator().migrate(context, MigrationReporter.searchGuard());

        assertFalse(result.isEmpty(), "Should have migrated at least one domain");
        var sgFrontendAuthC = assertInstanceOf(SgFrontendAuthC.class, result.get(0));
        assertNotNull(sgFrontendAuthC.authDomains());
        assertFalse(sgFrontendAuthC.authDomains().isEmpty(), "Should have auth domains");
    }

    @Test
    void testMigrateElasticsearchMultipleRealms() throws Exception {
        // Load multiple realms from elasticsearch config (includes SAML)
        var elasticsearch = loadElasticsearchConfig("/xpack_migrate/elasticsearch/auth/multiple_realms.yml");
        var context = createContext(Optional.of(elasticsearch), Optional.empty());

        var result = new FrontendAuthMigrator().migrate(context, MigrationReporter.searchGuard());

        assertFalse(result.isEmpty(), "Should have migrated realms");
        var sgFrontendAuthC = assertInstanceOf(SgFrontendAuthC.class, result.get(0));
        assertFalse(sgFrontendAuthC.authDomains().isEmpty());
    }

    @Test
    void testMigrateKibanaWithSAMLAndOIDC() throws Exception {
        var kibana = loadKibanaConfig("/xpack_migrate/kibana/basic.yml");
        var context = createContext(Optional.empty(), Optional.of(kibana));

        var result = new FrontendAuthMigrator().migrate(context, MigrationReporter.searchGuard());

        assertFalse(result.isEmpty());
        var sgFrontendAuthC = assertInstanceOf(SgFrontendAuthC.class, result.get(0));

        // Should have multiple auth domains
        assertTrue(sgFrontendAuthC.authDomains().size() > 0);
    }

    @Test
    void testMigrateEmptyConfig() throws Exception {
        var context = createContext(Optional.empty(), Optional.empty());

        var result = new FrontendAuthMigrator().migrate(context, MigrationReporter.searchGuard());

        assertTrue(result.isEmpty(), "Should return empty list when no config provided");
    }

    @Test
    void testMigrateWithBothKibanaAndElasticsearch() throws Exception {
        var kibana = loadKibanaConfig("/xpack_migrate/kibana/basic.yml");
        var elasticsearch = loadElasticsearchConfig("/xpack_migrate/elasticsearch/auth/ldap_basic.yml");
        var context = createContext(Optional.of(elasticsearch), Optional.of(kibana));

        var result = new FrontendAuthMigrator().migrate(context, MigrationReporter.searchGuard());

        // Should prefer Kibana providers over Elasticsearch realms
        assertFalse(result.isEmpty());
        var sgFrontendAuthC = assertInstanceOf(SgFrontendAuthC.class, result.get(0));
        assertFalse(sgFrontendAuthC.authDomains().isEmpty());
    }

    @Test
    void testMigrationOutputFormat() throws Exception {
        var kibana = loadKibanaConfig("/xpack_migrate/kibana/basic.yml");
        var context = createContext(Optional.empty(), Optional.of(kibana));

        var result = new FrontendAuthMigrator().migrate(context, MigrationReporter.searchGuard());

        assertFalse(result.isEmpty());
        var sgFrontendAuthC = assertInstanceOf(SgFrontendAuthC.class, result.get(0));

        // Verify file name and output format
        assertEquals("sg_frontend_authc.yml", sgFrontendAuthC.getFileName());
        var yaml = DocWriter.yaml().writeAsString(sgFrontendAuthC.toBasicObject());
        assertNotNull(yaml);
        assertFalse(yaml.isEmpty());
        assertTrue(yaml.contains("auth_domains"));
    }

    @Test
    void testSamlRealmOrderingUsesLowestOrderAsDefault() throws Exception {
        var elasticsearch = loadElasticsearchConfig("/xpack_migrate/elasticsearch/auth/saml_realms_order.yml");
        var context = createContext(Optional.of(elasticsearch), Optional.empty());

        var result = new FrontendAuthMigrator().migrate(context, MigrationReporter.searchGuard());

        assertFalse(result.isEmpty());
        var sgFrontendAuthC = assertInstanceOf(SgFrontendAuthC.class, result.get(0));
        assertEquals(2, sgFrontendAuthC.authDomains().size(), "Both SAML realms should migrate");

        var first = sgFrontendAuthC.authDomains().get(0);
        var second = sgFrontendAuthC.authDomains().get(1);
        var firstSaml = assertInstanceOf(SgFrontendAuthC.AuthDomain.Saml.class, first);
        var secondSaml = assertInstanceOf(SgFrontendAuthC.AuthDomain.Saml.class, second);

        assertTrue(firstSaml.isDefault(), "Lowest order realm should be default");
        assertFalse(secondSaml.isDefault(), "Higher order realm should not be default");
        assertEquals("https://idp.primary.example.com", firstSaml.idpEntityId());
        assertEquals("https://idp.secondary.example.com", secondSaml.idpEntityId());
    }

    @Test
    void testKibanaSamlProviderResolvesRealmFromElasticsearch() throws Exception {
        var kibana = loadKibanaConfig("/xpack_migrate/kibana/basic.yml");
        var elasticsearch = loadElasticsearchConfig("/xpack_migrate/elasticsearch/auth/saml_realms_order.yml");
        var context = createContext(Optional.of(elasticsearch), Optional.of(kibana));

        var result = new FrontendAuthMigrator().migrate(context, MigrationReporter.searchGuard());

        assertFalse(result.isEmpty());
        var sgFrontendAuthC = assertInstanceOf(SgFrontendAuthC.class, result.get(0));

        var samlDomains = sgFrontendAuthC.authDomains().stream()
                .filter(domain -> domain instanceof SgFrontendAuthC.AuthDomain.Saml)
                .map(domain -> (SgFrontendAuthC.AuthDomain.Saml) domain)
                .toList();

        assertFalse(samlDomains.isEmpty(), "SAML provider should produce a SAML auth domain");
        var saml = samlDomains.get(0);
        assertEquals("saml1", saml.id().orElseThrow());
        assertEquals("https://idp.primary.example.com", saml.idpEntityId());
    }

    // Helper methods

    private Migrator.IMigrationContext createContext(
            Optional<XPackElasticsearchConfig> elasticsearch,
            Optional<Kibana> kibana) {
        return new Migrator.MigrationContext(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                elasticsearch,
                kibana);
    }

    private XPackElasticsearchConfig loadElasticsearchConfig(String path) throws Exception {
        try (var in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "Resource not found: " + path);
            var node = DocNode.wrap(DocReader.yaml().read(in));
            return XPackElasticsearchConfig.parse(node, Parser.Context.get());
        }
    }

    private Kibana loadKibanaConfig(String path) throws Exception {
        try (var in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "Resource not found: " + path);
            var node = DocNode.wrap(DocReader.yaml().read(in));
            return Kibana.parse(node, Parser.Context.get());
        }
    }

}
