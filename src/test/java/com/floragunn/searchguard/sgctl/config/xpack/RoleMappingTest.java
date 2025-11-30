package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings.RoleMapping.Roles;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings.RoleMapping.Rule.Any;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings.RoleMapping.Rule.Field;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

// TODO: tests for template-based role mappings, requires some examples
public class RoleMappingTest {

  @Test
  public void testParseSimple() throws IOException, ConfigValidationException {
    var node = read("/xpack_migrate/role_mapping/simple.json");
    var mappings = RoleMappings.parse(node, Parser.Context.get());

    assertEquals(
        new RoleMappings(
            ImmutableMap.of(
                "admins",
                new Roles(
                    true,
                    ImmutableList.of("monitoring", "user"),
                    new Field(jsonNode("{\"groups\": \"cn=admins,dc=example,dc=com\"}")),
                    DocNode.EMPTY))),
        mappings);
  }

  @Test
  public void testParseWithLogic() throws IOException, ConfigValidationException {
    var node = read("/xpack_migrate/role_mapping/with_logic.json");
    var mappings = RoleMappings.parse(node, Parser.Context.get());

    assertEquals(
        new RoleMappings(
            ImmutableMap.of(
                "basic_users",
                new Roles(
                    true,
                    ImmutableList.of("user"),
                    new Any(
                        ImmutableList.of(
                            new Field(
                                jsonNode(
                                    "{\"dn\": \"cn=John Doe,cn=contractors,dc=example,dc=com\"}")),
                            new Field(jsonNode("{\"groups\": \"cn=users,dc=example,dc=com\"}")))),
                    DocNode.EMPTY))),
        mappings);
  }

  @Test
  public void testInvalidTooManyRules() throws IOException, DocumentParseException {
    var node = read("/xpack_migrate/role_mapping/invalid_too_many_rules.json");
    assertThrows(
        ConfigValidationException.class, () -> RoleMappings.parse(node, Parser.Context.get()));
  }

  @Test
  public void testInvalidNoRules() throws IOException, DocumentParseException {
    var node = read("/xpack_migrate/role_mapping/invalid_no_rules.json");
    assertThrows(
        ConfigValidationException.class, () -> RoleMappings.parse(node, Parser.Context.get()));
  }

  private DocNode read(String path) throws IOException, DocumentParseException {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in);
      return DocNode.wrap(DocReader.json().read(in));
    }
  }

  private DocNode jsonNode(String json) throws DocumentParseException {
    return DocNode.wrap(DocReader.json().read(json));
  }
}
