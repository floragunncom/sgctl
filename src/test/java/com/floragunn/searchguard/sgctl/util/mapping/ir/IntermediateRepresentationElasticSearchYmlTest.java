/*
 * Copyright 2025-2026 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */


package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.IntermediateRepresentationElasticSearchYml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link IntermediateRepresentationElasticSearchYml}.
 */
@SuppressWarnings("deprecation")
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
