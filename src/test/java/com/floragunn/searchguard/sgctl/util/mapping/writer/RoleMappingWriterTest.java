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


package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.testsupport.QuietTestBase;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RoleMappingWriter}.
 */
class RoleMappingWriterTest extends QuietTestBase {

    /**
     * Verifies role mappings with the same role name are merged.
     */
    @Test
    void shouldMergeRoleMappingsByRoleName() {
        IntermediateRepresentation ir = new IntermediateRepresentation();

        RoleMapping mapping1 = new RoleMapping("mapping1");
        mapping1.setRoles(List.of("role1"));
        RoleMapping.Rules rules1 = new RoleMapping.Rules();
        Map<String, Object> field1 = new HashMap<>();
        field1.put("username", "user1");
        field1.put("groups", List.of("backend1"));
        rules1.setField(field1);
        mapping1.setRules(rules1);

        RoleMapping mapping2 = new RoleMapping("mapping2");
        mapping2.setRoles(List.of("role1"));
        RoleMapping.Rules rules2 = new RoleMapping.Rules();
        Map<String, Object> field2 = new HashMap<>();
        field2.put("username", List.of("/a&b/"));
        rules2.setAny(List.of(rulesFromField(field2)));
        mapping2.setRules(rules2);

        RoleMapping mappingDisabled = new RoleMapping("mapping-disabled");
        mappingDisabled.setRoles(List.of("role2"));
        mappingDisabled.setEnabled(false);

        ir.addRoleMapping(mapping1);
        ir.addRoleMapping(mapping2);
        ir.addRoleMapping(mappingDisabled);

        RoleMappingWriter writer = new RoleMappingWriter(ir);

        Object basicObject = writer.toBasicObject();
        assertTrue(basicObject instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, RoleMappingWriter.SGRoleMapping> mappings =
                (Map<String, RoleMappingWriter.SGRoleMapping>) basicObject;

        assertTrue(mappings.containsKey("role1"));
        assertFalse(mappings.containsKey("role2"));

        RoleMappingWriter.SGRoleMapping roleMapping = mappings.get("role1");
        assertEquals(List.of("user1", "/a&b/"), roleMapping.users);
        assertEquals(List.of("backend1"), roleMapping.backendRoles);
        assertEquals(new ArrayList<>(), roleMapping.hosts);
        assertEquals(new ArrayList<>(), roleMapping.ips);
    }

    /**
     * Ensures non-string values in rules are ignored in the output.
     */
    @Test
    void shouldIgnoreInvalidTypesInRules() {
        IntermediateRepresentation ir = new IntermediateRepresentation();

        RoleMapping mapping = new RoleMapping("mapping");
        mapping.setRoles(List.of("roleA"));
        RoleMapping.Rules rules = new RoleMapping.Rules();
        Map<String, Object> field = new HashMap<>();
        field.put("username", List.of("userA", 7));
        field.put("groups", List.of(5, "backendA"));
        rules.setField(field);
        mapping.setRules(rules);
        ir.addRoleMapping(mapping);

        RoleMappingWriter writer = new RoleMappingWriter(ir);
        @SuppressWarnings("unchecked")
        Map<String, RoleMappingWriter.SGRoleMapping> mappings =
                (Map<String, RoleMappingWriter.SGRoleMapping>) writer.toBasicObject();

        RoleMappingWriter.SGRoleMapping roleMapping = mappings.get("roleA");
        assertEquals(List.of("userA"), roleMapping.users);
        assertEquals(List.of("backendA"), roleMapping.backendRoles);
    }

    /**
     * Ensures role mappings with role templates are skipped.
     */
    @Test
    void shouldSkipMappingsWithRoleTemplates() {
        IntermediateRepresentation ir = new IntermediateRepresentation();

        RoleMapping mapping = new RoleMapping("mapping");
        mapping.setRoles(List.of("roleA"));
        RoleMapping.RoleTemplate template = new RoleMapping.RoleTemplate();
        mapping.setRoleTemplates(List.of(template));
        ir.addRoleMapping(mapping);

        RoleMappingWriter writer = new RoleMappingWriter(ir);
        @SuppressWarnings("unchecked")
        Map<String, RoleMappingWriter.SGRoleMapping> mappings =
                (Map<String, RoleMappingWriter.SGRoleMapping>) writer.toBasicObject();

        assertFalse(mappings.containsKey("roleA"));
    }

    /**
     * Verifies invalid Lucene regex inputs are reported as manual actions.
     */
    @Test
    void shouldReportInvalidLuceneRegexInRules() {
        MigrationReport report = MigrationReport.shared;
        report.clear();
        try {
            IntermediateRepresentation ir = new IntermediateRepresentation();

            RoleMapping mapping = new RoleMapping("mapping-invalid");
            mapping.setRoles(List.of("roleA"));
            RoleMapping.Rules rules = new RoleMapping.Rules();
            Map<String, Object> field = new HashMap<>();
            field.put("username", "/a~b/");
            rules.setField(field);
            mapping.setRules(rules);
            ir.addRoleMapping(mapping);

            new RoleMappingWriter(ir);

            assertTrue(report.getEntries("sg_roles_mapping.yml", MigrationReport.Category.MANUAL)
                    .stream()
                    .anyMatch(entry -> "mapping-invalid->rules.field.username".equals(entry.parameter())));
        } finally {
            report.clear();
        }
    }

    /**
     * Builds rules with a field map for reuse in tests.
     *
     * @param field field values for the rules
     * @return rules containing the provided field map
     */
    private RoleMapping.Rules rulesFromField(Map<String, Object> field) {
        RoleMapping.Rules rules = new RoleMapping.Rules();
        rules.setField(field);
        return rules;
    }
}
