package com.floragunn.searchguard.sgctl.config.xpack;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.Source;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.trace.TraceableDocNode;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings.RoleMapping.Roles;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings.RoleMapping.Rule.Any;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings.RoleMapping.Rule.Field;
import java.io.IOException;
import org.junit.jupiter.api.Test;

// TODO: tests for template-based role mappings, requires some examples
public class RoleMappingTest {

  @Test
  public void testParseSimple() throws IOException, ConfigValidationException {
    var node = read("/xpack_migrate/role_mapping/simple.json");
    var mappings = parseRoleMappings(node);

    var fileSource = new Source.Config("role_mappings.json");
    var adminsSource = new Source.Attribute(fileSource, "admins");
    var enabledSource = new Source.Attribute(adminsSource, "enabled");
    var rolesSource = new Source.Attribute(adminsSource, "roles");
    var rulesSource = new Source.Attribute(adminsSource, "rules");
    var fieldSource = new Source.Attribute(rulesSource, "field");
    var groupsSource = new Source.Attribute(fieldSource, "groups");
    var metadataSource = new Source.Attribute(adminsSource, "metadata");

    Traceable<Object> groupsValue = Traceable.of(groupsSource, "cn=admins,dc=example,dc=com");
    var fieldMatch = Traceable.of(fieldSource, ImmutableMap.of("groups", groupsValue));
    var fieldRule = new Field(fieldMatch);

    var adminsMapping =
        new Roles(
            Traceable.of(enabledSource, true),
            Traceable.of(rolesSource, Traceable.ofList(rolesSource, "monitoring", "user")),
            Traceable.of(rulesSource, fieldRule),
            Traceable.of(metadataSource, ImmutableMap.empty()));

    var expected =
        new RoleMappings(
            Traceable.of(
                fileSource, ImmutableMap.of("admins", Traceable.of(adminsSource, adminsMapping))));

    assertEquals(expected, mappings);
  }

  @Test
  public void testParseWithLogic() throws IOException, ConfigValidationException {
    var node = read("/xpack_migrate/role_mapping/with_logic.json");
    var mappings = parseRoleMappings(node);

    var fileSource = new Source.Config("role_mappings.json");
    var basicUsersSource = new Source.Attribute(fileSource, "basic_users");
    var enabledSource = new Source.Attribute(basicUsersSource, "enabled");
    var rolesSource = new Source.Attribute(basicUsersSource, "roles");
    var rulesSource = new Source.Attribute(basicUsersSource, "rules");
    var anySource = new Source.Attribute(rulesSource, "any");
    var rule0Source = new Source.ListEntry(anySource, 0);
    var field0Source = new Source.Attribute(rule0Source, "field");
    var dnSource = new Source.Attribute(field0Source, "dn");
    var rule1Source = new Source.ListEntry(anySource, 1);
    var field1Source = new Source.Attribute(rule1Source, "field");
    var groupsSource = new Source.Attribute(field1Source, "groups");
    var metadataSource = new Source.Attribute(basicUsersSource, "metadata");

    Traceable<Object> dnValue =
        Traceable.of(dnSource, "cn=John Doe,cn=contractors,dc=example,dc=com");
    var field0Match = Traceable.of(field0Source, ImmutableMap.of("dn", dnValue));
    var field0Rule = new Field(field0Match);

    Traceable<Object> groupsValue = Traceable.of(groupsSource, "cn=users,dc=example,dc=com");
    var field1Match = Traceable.of(field1Source, ImmutableMap.of("groups", groupsValue));
    var field1Rule = new Field(field1Match);

    var anyRules =
        Traceable.of(
            anySource,
            Traceable.<RoleMappings.RoleMapping.Rule>ofList(anySource, field0Rule, field1Rule));
    var anyRule = new Any(anyRules);

    var basicUsersMapping =
        new Roles(
            Traceable.of(enabledSource, true),
            Traceable.of(rolesSource, Traceable.ofList(rolesSource, "user")),
            Traceable.of(rulesSource, anyRule),
            Traceable.of(metadataSource, ImmutableMap.empty()));

    var expected =
        new RoleMappings(
            Traceable.of(
                fileSource,
                ImmutableMap.of("basic_users", Traceable.of(basicUsersSource, basicUsersMapping))));

    assertEquals(expected, mappings);
  }

  @Test
  public void testInvalidTooManyRules() throws IOException, DocumentParseException {
    var node = read("/xpack_migrate/role_mapping/invalid_too_many_rules.json");
    assertThrows(ConfigValidationException.class, () -> parseRoleMappings(node));
  }

  @Test
  public void testInvalidNoRules() throws IOException, DocumentParseException {
    var node = read("/xpack_migrate/role_mapping/invalid_no_rules.json");
    assertThrows(ConfigValidationException.class, () -> parseRoleMappings(node));
  }

  private DocNode read(String path) throws IOException, DocumentParseException {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in);
      return DocNode.wrap(DocReader.json().read(in));
    }
  }

  private RoleMappings parseRoleMappings(DocNode node) throws ConfigValidationException {
    var src = new Source.Config("role_mappings.json");
    return TraceableDocNode.parse(node, src, RoleMappings::parse);
  }
}
