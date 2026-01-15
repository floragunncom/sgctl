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

import com.floragunn.searchguard.sgctl.testsupport.TestBase;
import com.floragunn.searchguard.sgctl.util.mapping.reader.XPackConfigReader;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.RoleMapping;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.User;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link XPackConfigReader} that match the current implementation.
 * For not yet implemented features, additional disabled tests are provided as TODOs.
 */
class XPackConfigReaderTest extends TestBase {

    /**
     * Verifies that an empty configuration returns an empty intermediate representation.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldCreateEmptyIntermediateRepresentationWhenAllFilesAreNull(@TempDir Path tempDir) throws IOException {
        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                null,
                null,
                null
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertNotNull(ir);
        assertTrue(ir.getUsers().isEmpty());
        assertTrue(ir.getRoles().isEmpty());
        assertTrue(ir.getRoleMappings().isEmpty());
    }

    /**
     * Verifies that valid roles and related index settings are parsed according to the current implementation.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldParseValidRoles(@TempDir Path tempDir) throws IOException {
        IntermediateRepresentation ir = createIrFromValidConfiguration(tempDir);

        // Roles
        assertEquals(1, ir.getRoles().size());
        Role admin = findRole(ir, "admin");
        assertEquals("admin", admin.getName());
        assertEquals(List.of("monitor"), admin.getCluster());

        assertNotNull(admin.getIndices());
        assertEquals(1, admin.getIndices().size());
        Role.Index index = admin.getIndices().get(0);
        assertEquals(List.of("logs-*"), index.getNames());
        assertEquals(List.of("read"), index.getPrivileges());
        assertNotNull(index.getFieldSecurity());
        assertEquals(List.of("field1"), index.getFieldSecurity().getGrant());
        assertEquals(List.of("field2"), index.getFieldSecurity().getExcept());
        assertEquals("user.name:test", index.getQuery());

        assertNotNull(admin.getRemoteIndices());
        assertEquals(1, admin.getRemoteIndices().size());
        Role.RemoteIndex remoteIndex = admin.getRemoteIndices().get(0);
        assertEquals(List.of("remote-cluster"), remoteIndex.getCluster());
        assertEquals(List.of("remote-*"), remoteIndex.getNames());
        assertEquals(List.of("read"), remoteIndex.getPrivileges());
        assertEquals(Boolean.TRUE, remoteIndex.isAllowRestrictedIndices());

        assertEquals(List.of("other-user"), admin.getRunAs());
        assertEquals("Admin role with monitoring and read access to logs indices.", admin.getDescription());
    }

    /**
     * Verifies that valid users are parsed according to the current implementation.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldParseValidUsers(@TempDir Path tempDir) throws IOException {
        IntermediateRepresentation ir = createIrFromValidConfiguration(tempDir);

        assertEquals(1, ir.getUsers().size());
        User user = findUser(ir, "test-user");
        assertEquals("test-user", user.getUsername());
        assertEquals("Test User", user.getFullName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(List.of("admin"), user.getRoles());
    }

    /**
     * Verifies that valid role mappings are parsed according to the current implementation.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldParseValidRoleMappings(@TempDir Path tempDir) throws IOException {
        IntermediateRepresentation ir = createIrFromValidConfiguration(tempDir);

        assertEquals(1, ir.getRoleMappings().size());
        RoleMapping mapping = findRoleMapping(ir, "mapping1");
        assertTrue(mapping.isEnabled());
        assertEquals(List.of("admin"), mapping.getRoles());
        assertEquals(List.of("other-user"), mapping.getRunAs());

        // Current implementation only sets a non-null Rules instance without details.
        assertNotNull(mapping.getRules());

        // Metadata is retained for review; roleTemplates remain unset.
        assertNotNull(mapping.getMetadata());
        assertNull(mapping.getRoleTemplates());
    }

    /**
     * Verifies non-object JSON roots are reported as invalid types.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportInvalidUserJsonRootType(@TempDir Path tempDir) throws IOException {
        MigrationReport report = newEmptyReport();
        Path usersFile = tempDir.resolve("users-invalid.json");
        Files.writeString(usersFile, "\"invalid\"");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            XPackConfigReader reader = new XPackConfigReader(
                    writeEmptyElasticsearchFile(tempDir),
                    usersFile.toFile(),
                    null,
                    null
            );
            generateIr(reader);
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("user.json", MigrationReport.Category.WARNING)
                .stream()
                .anyMatch(entry -> "origin".equals(entry.getParameter())));
    }

    /**
     * Verifies invalid user entry types are reported.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldReportInvalidUserEntryType(@TempDir Path tempDir) throws IOException {
        MigrationReport report = newEmptyReport();
        Path usersFile = tempDir.resolve("users-invalid-type.json");
        Files.writeString(usersFile, "{\"user1\":\"oops\"}");

        MigrationReport previous = MigrationReport.shared;
        try {
            MigrationReport.shared = report;
            XPackConfigReader reader = new XPackConfigReader(
                    writeEmptyElasticsearchFile(tempDir),
                    usersFile.toFile(),
                    null,
                    null
            );
            generateIr(reader);
        } finally {
            MigrationReport.shared = previous;
        }

        assertTrue(report.getEntries("user.json", MigrationReport.Category.WARNING)
                .stream()
                .anyMatch(entry -> "user1".equals(entry.getParameter())));
    }

    /**
     * Creates an intermediate representation from the valid configuration JSON resources.
     *
     * @param tempDir temporary directory provided by JUnit
     * @return intermediate representation built by the reader
     */
    private IntermediateRepresentation createIrFromValidConfiguration(Path tempDir) throws IOException {
        Path rolesFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/roles-valid.json");
        Path usersFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/users-valid.json");
        Path mappingsFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/role-mappings-valid.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                usersFile.toFile(),
                rolesFile.toFile(),
                mappingsFile.toFile()
        );

        IntermediateRepresentation ir = generateIr(reader);
        assertNotNull(ir);
        return ir;
    }

    /**
     * Generates an intermediate representation and wraps checked exceptions.
     *
     * @param reader reader instance
     * @return generated intermediate representation
     */
    private IntermediateRepresentation generateIr(XPackConfigReader reader) {
        try {
            return reader.generateIR();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate intermediate representation", exception);
        }
    }

    /**
     * Writes an empty elasticsearch.yml file for reader initialization.
     *
     * @param tempDir temporary directory
     * @return file handle to the created config
     */
    private File writeEmptyElasticsearchFile(Path tempDir) throws IOException {
        Path elasticsearchFile = tempDir.resolve("elasticsearch.yml");
        Files.writeString(elasticsearchFile, "");
        return elasticsearchFile.toFile();
    }

    /**
     * Creates a new migration report instance without using the shared singleton.
     *
     * @return empty migration report
     */
    private MigrationReport newEmptyReport() {
        try {
            var ctor = MigrationReport.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create isolated MigrationReport", e);
        }
    }

    /**
     * Verifies that a user whose username does not match the JSON key is skipped.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldSkipUserWhenUsernameDoesNotMatchKey(@TempDir Path tempDir) throws IOException {
        Path rolesFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/roles-valid.json");
        Path usersFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/users-mismatch-username.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                usersFile.toFile(),
                rolesFile.toFile(),
                null
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertEquals(1, ir.getRoles().size());
        assertTrue(ir.getUsers().isEmpty());
    }

    /**
     * Verifies that unknown roles in the user definition are ignored and known roles are kept.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldIgnoreUnknownRolesInUser(@TempDir Path tempDir) throws IOException {
        Path rolesFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/roles-valid.json");
        Path usersFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/users-unknown-role.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                usersFile.toFile(),
                rolesFile.toFile(),
                null
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertEquals(1, ir.getUsers().size());
        User user = findUser(ir, "test-user");
        assertEquals(List.of("admin"), user.getRoles());
    }

    /**
     * Verifies that a non-list roles value in the user definition is handled gracefully by leaving roles unset.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldHandleInvalidRolesTypeInUser(@TempDir Path tempDir) throws IOException {
        Path rolesFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/roles-valid.json");
        Path usersFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/users-invalid-roles-type.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                usersFile.toFile(),
                rolesFile.toFile(),
                null
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertEquals(1, ir.getUsers().size());
        User user = findUser(ir, "test-user");
        assertNull(user.getRoles());
    }

    /**
     * Verifies that a user file whose root element is not a map is ignored.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldIgnoreUserFileWhenRootIsNotMap(@TempDir Path tempDir) throws IOException {
        Path usersFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/users-root-array.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                usersFile.toFile(),
                null,
                null
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertTrue(ir.getUsers().isEmpty());
    }

    /**
     * Verifies that invalid JSON in the user file does not break parsing and results in no users.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldHandleInvalidUserJsonGracefully(@TempDir Path tempDir) throws IOException {
        Path usersFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/invalid-json.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                usersFile.toFile(),
                null,
                null
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertTrue(ir.getUsers().isEmpty());
    }

    /**
     * Verifies that invalid JSON in the role file does not break parsing and results in no roles.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldHandleInvalidRoleJsonGracefully(@TempDir Path tempDir) throws IOException {
        Path rolesFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/invalid-json.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                null,
                rolesFile.toFile(),
                null
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertTrue(ir.getRoles().isEmpty());
    }

    /**
     * Verifies that invalid JSON in the role-mapping file does not break parsing and results in no role mappings.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldHandleInvalidRoleMappingJsonGracefully(@TempDir Path tempDir) throws IOException {
        Path mappingsFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/invalid-json.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                null,
                null,
                mappingsFile.toFile()
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertTrue(ir.getRoleMappings().isEmpty());
    }

    /**
     * Verifies that a role JSON with an invalid indices structure is handled gracefully by leaving indices unset.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldHandleRoleWithInvalidIndicesType(@TempDir Path tempDir) throws IOException {
        Path rolesFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/roles-invalid-indices.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                null,
                rolesFile.toFile(),
                null
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertEquals(1, ir.getRoles().size());
        Role role = findRole(ir, "role1");
        assertNull(role.getIndices());
    }

    /**
     * Verifies that invalid structures in role mappings are handled gracefully without throwing exceptions.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldHandleRoleMappingWithInvalidTypes(@TempDir Path tempDir) throws IOException {
        Path rolesFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/roles-valid.json");
        Path usersFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/users-valid.json");
        Path mappingsFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/role-mappings-invalid-types.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                usersFile.toFile(),
                rolesFile.toFile(),
                mappingsFile.toFile()
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertFalse(ir.getRoleMappings().isEmpty());
        // for invalid entries we only check that no exception is thrown and IR is still usable
    }

    /**
     * Verifies that a valid rules object creates a non-null Rules instance.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldCreateRulesObjectForValidRulesMap(@TempDir Path tempDir) throws IOException {
        Path mappingsFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/role-mappings-valid.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                null,
                null,
                mappingsFile.toFile()
        );

        IntermediateRepresentation ir = generateIr(reader);

        RoleMapping mapping = findRoleMapping(ir, "mapping1");
        assertNotNull(mapping.getRules());
        // TODO: Once readRules() is fully implemented, add assertions for fields like field/any/all/except
    }

    /**
     * Verifies that an invalid type for the rules object yields a null Rules instance.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldIgnoreRulesWhenTypeIsInvalid(@TempDir Path tempDir) throws IOException {
        Path mappingsFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/role-mappings-invalid-types.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                null,
                null,
                mappingsFile.toFile()
        );

        IntermediateRepresentation ir = generateIr(reader);

        RoleMapping mapping = findRoleMapping(ir, "mappingWithInvalidRulesType");
        assertNull(mapping.getRules());
    }

    /**
     * Verifies that roleTemplates is at least a non-null list when role_templates is configured.
     * The current implementation returns an empty list for any templates.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldProduceEmptyRoleTemplatesListWhenTemplatesAreConfigured(@TempDir Path tempDir) throws IOException {
        Path mappingsFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/role-mappings-with-role-templates.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                null,
                null,
                mappingsFile.toFile()
        );

        IntermediateRepresentation ir = generateIr(reader);

        RoleMapping mapping = findRoleMapping(ir, "mappingWithTemplates");
        assertNotNull(mapping.getRoleTemplates());
        assertTrue(mapping.getRoleTemplates().isEmpty());
        // TODO: Once readRoleTemplates() is implemented, assert the actual template content here
    }

    /**
     * Verifies that missing files are handled via the FileNotFoundException branches and result in an empty IR.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldHandleMissingFilesGracefully(@TempDir Path tempDir) throws IOException {
        File missingUsers = tempDir.resolve("missing-users.json").toFile();
        File missingRoles = tempDir.resolve("missing-roles.json").toFile();
        File missingMappings = tempDir.resolve("missing-mappings.json").toFile();

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                missingUsers,
                missingRoles,
                missingMappings
        );

        IntermediateRepresentation ir = generateIr(reader);

        assertNotNull(ir);
        assertTrue(ir.getUsers().isEmpty());
        assertTrue(ir.getRoles().isEmpty());
        assertTrue(ir.getRoleMappings().isEmpty());
    }

    /**
     * Disabled test that describes the expected behavior once metadata in role mappings is implemented.
     *
     * @param tempDir temporary directory provided by JUnit
     */
    @Test
    void shouldParseMetadataInRoleMapping(@TempDir Path tempDir) throws IOException {
        Path mappingsFile = writeJsonResourceToTempFile(tempDir, "testbase/xpack/role-mappings-valid.json");

        XPackConfigReader reader = new XPackConfigReader(
                writeEmptyElasticsearchFile(tempDir),
                null,
                null,
                mappingsFile.toFile()
        );

        IntermediateRepresentation ir = generateIr(reader);

        RoleMapping mapping = findRoleMapping(ir, "mapping1");
        assertNotNull(mapping.getMetadata());
        assertEquals("simple mapping for sgctl tests", mapping.getMetadata().getEntries().get("note"));
    }

    /**
     * Writes a JSON test resource to a temporary file.
     *
     * @param tempDir temporary directory provided by JUnit
     * @param resourceName classpath resource name under src/test/resources
     * @return created file path
     */
    private Path writeJsonResourceToTempFile(Path tempDir, String resourceName) throws IOException {
        String json = readResourceAsString(resourceName);
        Path file = tempDir.resolve(Path.of(resourceName).getFileName().toString());
        Files.writeString(file, json);
        return file;
    }

    /**
     * Finds a role with the given name in the intermediate representation.
     *
     * @param ir intermediate representation to search
     * @param roleName role name
     * @return found role
     */
    private Role findRole(IntermediateRepresentation ir, String roleName) {
        return ir.getRoles().stream()
                .filter(role -> roleName.equals(role.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Role not found: " + roleName));
    }

    /**
     * Finds a user with the given username in the intermediate representation.
     *
     * @param ir intermediate representation to search
     * @param username username
     * @return found user
     */
    private User findUser(IntermediateRepresentation ir, String username) {
        return ir.getUsers().stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("User not found: " + username));
    }

    /**
     * Finds a role mapping with the given name in the intermediate representation.
     *
     * @param ir intermediate representation to search
     * @param mappingName mapping name
     * @return found role mapping
     */
    private RoleMapping findRoleMapping(IntermediateRepresentation ir, String mappingName) {
        return ir.getRoleMappings().stream()
                .filter(mapping -> mappingName.equals(mapping.getMappingName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Role mapping not found: " + mappingName));
    }
}
