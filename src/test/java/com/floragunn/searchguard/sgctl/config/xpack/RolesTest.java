package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Format;
import com.floragunn.codova.validation.ConfigValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RolesTest {

    @Test
    void testRoleParsing() throws Exception {
        String json = """
            {
              "complex_role": {
                "run_as": ["user1"],
                "cluster": ["monitor"],
                "global": { "test1": "test2" },
                "indices": [
                  {
                    "names": ["admin"],
                    "privileges": ["read"],
                    "query": "{\\"test\\": {}}",
                    "allow_restricted_indices": true
                  }
                ],
                "applications": [
                  {
                    "application": "kibana",
                    "privileges": ["read"],
                    "resources": ["*"]
                  }
                ],
                "remote_indices": [
                  {
                    "clusters": ["test-cluster1"],
                    "names": ["boss2"],
                    "privileges": ["read"]
                  }
                ],
                "remote_cluster": [
                   {
                     "clusters": ["test-cluster2"],
                     "privileges": ["all"]
                   }
                ],
                "metadata": {"version": 1},
                "description": "Beautiful description"
              }
            }
            """;

        DocNode docNode = DocNode.parse(Format.JSON).from(json);
        Roles result = Roles.parse(docNode, null);

        assertNotNull(result);
        assertTrue(result.roles().containsKey("complex_role"));

        var role = result.roles().get("complex_role");

        // General asserts
        assertEquals("user1", role.runAs().get(0));
        assertEquals("monitor", role.cluster().get(0));assertTrue(role.global().isPresent());
        assertEquals("test2", role.global().get().getAsString("test1"));

        // Indices
        assertEquals("admin", role.indices().get(0).names().get(0));
        assertEquals("read", role.indices().get(0).privileges().get(0));
        assertTrue(role.indices().get(0).allowRestrictedIndices());

        // Applications
        assertEquals("kibana", role.applications().get(0).application());
        assertEquals("read", role.applications().get(0).privileges().get(0));
        assertEquals("*", role.applications().get(0).resources().get(0));

        // Remote indices
        assertEquals("test-cluster1", role.remoteIndices().get(0).clusters().get(0));
        assertEquals("boss2", role.remoteIndices().get(0).names().get(0));
        assertEquals("read", role.remoteIndices().get(0).privileges().get(0));

        // Remote cluster
        assertEquals("test-cluster2", role.remoteCluster().get(0).clusters().get(0));
        assertEquals("all", role.remoteCluster().get(0).privileges().get(0));

        // Rest
        assertTrue(role.description().isPresent());
        assertEquals("Beautiful description", role.description().get());
    }

    @Test
    void testSmallRoleParsing() throws Exception {
        String json = """
            {
              "minimal_role": {
                "cluster": ["all"],
                "indices": [],
                "applications": [],
                "metadata": {}
              }
            }
            """;

        DocNode docNode = DocNode.parse(Format.JSON).from(json);
        Roles result = Roles.parse(docNode, null);

        var role = result.roles().get("minimal_role");

        assertEquals("all", role.cluster().get(0));
        assertTrue(role.indices().isEmpty());
        assertTrue(role.applications().isEmpty());
        assertTrue(role.runAs().isEmpty());
        assertTrue(role.description().isEmpty());
    }

    @Test
    void testEmptyDocumentParsing() throws Exception {
        DocNode docNode = DocNode.parse(Format.JSON).from("{}");
        Roles result = Roles.parse(docNode, null);

        assertNotNull(result);
        assertTrue(result.roles().isEmpty());
    }

    @Test
    void testMissingRequiredFields() throws DocumentParseException {
        // indices and applications (required) missing
        String json = """
            {
              "broken_role": {
                "cluster": ["all"],
                "metadata": {}
              }
            }
            """;

        DocNode docNode = DocNode.parse(Format.JSON).from(json);

        assertThrows(ConfigValidationException.class, () -> {
            Roles.parse(docNode, null);
        });
    }

    @Test
    void testMissingNestedRequiredField() throws DocumentParseException {
        // applications.privileges (required) missing
        String json = """
            {
              "broken_nested_role": {
                "cluster": [], "indices": [], "metadata": {},
                "applications": [
                  {
                    "application": "kibana",
                    "resources": ["*"]
                  }
                ]
              }
            }
            """;

        DocNode docNode = DocNode.parse(Format.JSON).from(json);

        assertThrows(ConfigValidationException.class, () -> {
            Roles.parse(docNode, null);
        });
    }
}