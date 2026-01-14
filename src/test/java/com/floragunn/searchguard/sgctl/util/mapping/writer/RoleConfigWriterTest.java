package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.testsupport.QuietTestBase;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;

import com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation.RealmTranslator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RoleConfigWriter}.
 */
class RoleConfigWriterTest extends QuietTestBase {

    /**
     * Verifies privilege mapping, index pattern conversion, and DLS/FLS handling.
     */
    @Test
    void shouldCreateRoleConfigWithConvertedPrivilegesAndQueries() {
        IntermediateRepresentation ir = new IntermediateRepresentation();

        Role.FieldSecurity fieldSecurity = new Role.FieldSecurity();
        fieldSecurity.setGrant(new ArrayList<>(List.of("field1")));
        fieldSecurity.setExcept(new ArrayList<>(List.of("secret")));

        Role.Index index = new Role.Index(
                List.of("/a&b/"),
                List.of("read", "write", "delete", "manage_ilm"),
                fieldSecurity,
                "{\"match\":{\"field\":\"value\"}}",
                false
        );

        Role role = new Role("role1");
        role.setDescription("Role description");
        role.setCluster(List.of("monitor", "manage_security"));
        role.setIndices(List.of(index));

        ir.addRole(role);

        ActionGroupConfigWriter agWriter = new ActionGroupConfigWriter();
        RoleConfigWriter writer = new RoleConfigWriter(ir, new SGAuthcTranslator.SgAuthc(new ArrayList<>(), null, null), agWriter);

        @SuppressWarnings("unchecked")
        Map<String, ActionGroupConfigWriter.ActionGroup> actionGroups =
                (Map<String, ActionGroupConfigWriter.ActionGroup>) agWriter.toBasicObject();
        assertTrue(actionGroups.containsKey("SGS_MANAGE_SECURITY_CUSTOM"));
        assertTrue(actionGroups.containsKey("SGS_MANAGE_ILM_CUSTOM"));

        Object basicObject = writer.toBasicObject();
        assertTrue(basicObject instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, RoleConfigWriter.SGRole> roles =
                (Map<String, RoleConfigWriter.SGRole>) basicObject;

        RoleConfigWriter.SGRole sgRole = roles.get("role1");
        assertNotNull(sgRole);
        assertEquals("Role description", sgRole.description);
        assertEquals(List.of("SGS_CLUSTER_MONITOR", "SGS_MANAGE_SECURITY_CUSTOM"), sgRole.clusterPermissions);

        assertEquals(1, sgRole.index.size());
        RoleConfigWriter.SGRole.SGIndex sgIndex = sgRole.index.get(0);
        assertEquals(List.of("/a&b/"), sgIndex.indexPatterns);
        assertEquals(List.of("SGS_READ", "SGS_WRITE", "SGS_DELETE", "SGS_MANAGE_ILM_CUSTOM"), sgIndex.allowedActions);
        assertEquals("{\"match\": {\"field\": \"value\"}}", sgIndex.dls);
        assertEquals(List.of("field1", "~secret"), sgIndex.fls);
    }

    /**
     * Ensures mustache-style templates in DLS are converted to Search Guard substitutions.
     */
    @Test
    void shouldConvertUserTemplatesInDlsQuery() {
        IntermediateRepresentation ir = new IntermediateRepresentation();

        Role.Index index = new Role.Index(
                List.of("logs-*"),
                List.of("read"),
                null,
                "{\"match\":{\"owner\":\"{{_user.username}}\"}}",
                false
        );

        Role role = new Role("role-template");
        role.setCluster(List.of("monitor"));
        role.setIndices(List.of(index));
        ir.addRole(role);

        ActionGroupConfigWriter agWriter = new ActionGroupConfigWriter();
        RoleConfigWriter writer = new RoleConfigWriter(ir, new SGAuthcTranslator.SgAuthc(new ArrayList<>(), null, null), agWriter);

        @SuppressWarnings("unchecked")
        Map<String, RoleConfigWriter.SGRole> roles =
                (Map<String, RoleConfigWriter.SGRole>) writer.toBasicObject();
        RoleConfigWriter.SGRole sgRole = roles.get("role-template");

        assertNotNull(sgRole);
        assertEquals(1, sgRole.index.size());
        RoleConfigWriter.SGRole.SGIndex sgIndex = sgRole.index.get(0);
        assertEquals("{\"match\": {\"owner\": \"${user.name}\"}}", sgIndex.dls);
    }

    /**
     * Verifies invalid Lucene regex in index patterns are reported as manual actions.
     */
    @Test
    void shouldReportInvalidLuceneRegexInIndexPattern() {
        MigrationReport report = MigrationReport.shared;
        report.clear();
        try {
            IntermediateRepresentation ir = new IntermediateRepresentation();

            Role role = new Role("role-invalid");
            role.setCluster(List.of("monitor"));
            role.setIndices(List.of(new Role.Index(List.of("/a~b/"), List.of("read"), null, null, false)));
            ir.addRole(role);

            ActionGroupConfigWriter agWriter = new ActionGroupConfigWriter();
            new RoleConfigWriter(ir, new SGAuthcTranslator.SgAuthc(new ArrayList<>(), null, null), agWriter);

            assertTrue(report.getEntries("sg_roles.yml", MigrationReport.Category.MANUAL)
                    .stream()
                    .anyMatch(entry -> "role-invalid->/a~b/".equals(entry.getParameter())));
        } finally {
            report.clear();
        }
    }
}
