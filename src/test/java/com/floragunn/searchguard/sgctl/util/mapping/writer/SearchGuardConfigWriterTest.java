package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.testsupport.QuietTestBase;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;
import com.floragunn.searchguard.sgctl.util.mapping.testsupport.IrTestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SearchGuardConfigWriter}.
 */
class SearchGuardConfigWriterTest extends QuietTestBase {

    /**
     * Verifies writer output creates expected YAML files.
     *
     * @param tempDir temporary directory for output
     */
    @Test
    void shouldWriteExpectedConfigFiles(@TempDir Path tempDir) throws IOException {
        IntermediateRepresentation ir = buildIntermediateRepresentation();
        SearchGuardConfigWriter writer = new SearchGuardConfigWriter(ir);

        writer.outputContent(tempDir.toFile());

        Path usersFile = tempDir.resolve(UserConfigWriter.FILE_NAME);
        Path rolesFile = tempDir.resolve(RoleConfigWriter.FILE_NAME);
        Path mappingsFile = tempDir.resolve(RoleMappingWriter.FILE_NAME);
        Path actionGroupsFile = tempDir.resolve(ActionGroupConfigWriter.FILE_NAME);

        assertTrue(Files.exists(usersFile));
        assertTrue(Files.exists(rolesFile));
        assertTrue(Files.exists(mappingsFile));
        assertTrue(Files.exists(actionGroupsFile));

        String usersContent = normalizeLineEndings(Files.readString(usersFile, StandardCharsets.UTF_8));
        String rolesContent = normalizeLineEndings(Files.readString(rolesFile, StandardCharsets.UTF_8));
        String mappingsContent = normalizeLineEndings(Files.readString(mappingsFile, StandardCharsets.UTF_8));

        assertTrue(usersContent.contains("user1:"));
        assertTrue(rolesContent.contains("role1:"));
        assertTrue(mappingsContent.contains("role1:"));
    }

    /**
     * Builds a minimal intermediate representation for writer integration testing.
     *
     * @return populated intermediate representation
     */
    private IntermediateRepresentation buildIntermediateRepresentation() {
        Role role = IrTestHelper.newRole(
                "role1",
                List.of("monitor"),
                List.of(new Role.Index(List.of("logs-*"), List.of("read"), null, null, false))
        );
        var user = IrTestHelper.newUser("user1", List.of("role1"), true);
        RoleMapping mapping = new RoleMapping("mapping1");
        mapping.setRoles(List.of("role1"));
        RoleMapping.Rules rules = new RoleMapping.Rules();
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("username", "user1");
        rules.setField(field);
        mapping.setRules(rules);
        return IrTestHelper.newIr(role, user, mapping);
    }
}
