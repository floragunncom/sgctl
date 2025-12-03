package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IntermediateRepresentationElasticSearchYml}.
 */
class IntermediateRepresentationElasticSearchYmlTest {

    /**
     * Verifies that the constructor initializes all subsections.
     */
    @Test
    void constructorShouldInitializeSections() {
        IntermediateRepresentationElasticSearchYml ir = new IntermediateRepresentationElasticSearchYml();

        assertNotNull(ir.getGlobal(), "Global section must be initialized");
        assertNotNull(ir.getSslTls(), "SSL/TLS section must be initialized");
        assertNotNull(ir.getAuthent(), "Authentication section must be initialized");
    }

    /**
     * Verifies that {@link IntermediateRepresentationElasticSearchYml#assertType(Object, Class)}
     * correctly distinguishes matching and non-matching types and that error logging does not throw.
     */
    @Test
    void assertTypeShouldReturnTrueOnlyForMatchingTypesAndErrorLogShouldNotThrow() {
        assertTrue(IntermediateRepresentationElasticSearchYml.assertType("value", String.class));
        assertFalse(IntermediateRepresentationElasticSearchYml.assertType("value", Integer.class));
        assertTrue(IntermediateRepresentationElasticSearchYml.assertType(Boolean.TRUE, Boolean.class));

        assertDoesNotThrow(() ->
                IntermediateRepresentationElasticSearchYml.errorLog("message", 0));
        assertDoesNotThrow(() ->
                IntermediateRepresentationElasticSearchYml.errorLog("manual", 1));
        assertDoesNotThrow(() ->
                IntermediateRepresentationElasticSearchYml.errorLog("critical", 2));
    }
}
