package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.testsupport.TestBase;
import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import com.floragunn.searchguard.sgctl.util.mapping.reader.ElasticsearchYamlReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link ElasticsearchYamlReader} populates the intermediate representation from an elasticsearch.yml.
 */
class ElasticsearchYamlReaderTest extends TestBase {

    @Test
    void shouldPopulateIntermediateRepresentationFromElasticsearchYaml() {
        Path configPath = resolveResourcePath("xpack_config/elasticsearch.yml");
        IntermediateRepresentationElasticSearchYml ir = new IntermediateRepresentationElasticSearchYml();

        new ElasticsearchYamlReader(new File(configPath.toString()), ir);

        assertFalse(ir.getGlobal().getXpackSecEnabled(), "xpack.security.enabled should remain false");

        var http = ir.getSslTls().getHttp();
        assertTrue(http.getEnabled(), "HTTP TLS should be enabled");
        assertEquals("certs/http.p12", http.getKeystorePath(), "HTTP keystore path should be parsed");

        var transport = ir.getSslTls().getTransport();
        assertTrue(transport.getEnabled(), "Transport TLS should be enabled");
        assertEquals("certificate", transport.getVerificationMode(), "Transport verification mode should be set");
        assertEquals("certs/transport.p12", transport.getKeystorePath(), "Transport keystore path should be parsed");
        assertEquals("certs/transport.p12", transport.getTruststorePath(), "Transport truststore path should be parsed");
    }
}
