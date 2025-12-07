/*
 * Copyright 2024 floragunn GmbH
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
 */

package com.floragunn.searchguard.sgctl.config.migrate.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.Parser;
import com.floragunn.searchguard.sgctl.config.migrate.Migrator;
import com.floragunn.searchguard.sgctl.config.searchguard.NamedConfig;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC;
import com.floragunn.searchguard.sgctl.config.searchguard.SgAuthC.AuthDomain;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings;
import com.floragunn.searchguard.sgctl.config.xpack.Roles;
import com.floragunn.searchguard.sgctl.config.xpack.Users;
import com.floragunn.searchguard.sgctl.config.xpack.XPackElasticsearchConfig;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests for {@link AuthMigrator}. */
class AuthMigratorTest {

  private static final Logger logger = LoggerFactory.getLogger(AuthMigratorTest.class);

  @Test
  void testMigrateNativeOnly() throws Exception {
    var sgAuthC = migrate("/xpack_migrate/elasticsearch/auth/native_only.yml");

    assertEquals(1, sgAuthC.authDomains().size());
    assertInstanceOf(AuthDomain.Internal.class, sgAuthC.authDomains().get(0));
  }

  @Test
  void testMigrateFileOnly() throws Exception {
    var sgAuthC = migrate("/xpack_migrate/elasticsearch/auth/file_only.yml");

    assertEquals(1, sgAuthC.authDomains().size());
    assertInstanceOf(AuthDomain.Internal.class, sgAuthC.authDomains().get(0));
  }

  @Test
  void testMigrateLdapBasic() throws Exception {
    var sgAuthC = migrate("/xpack_migrate/elasticsearch/auth/ldap_basic.yml");

    assertEquals(1, sgAuthC.authDomains().size());
    var ldap = assertLdapDomain(sgAuthC.authDomains().get(0));

    assertFalse(ldap.identityProvider().hosts().isEmpty());
    assertTrue(ldap.userSearch().isPresent());
    assertEquals("ou=users,dc=example,dc=com", ldap.userSearch().get().baseDn().orElse(null));
    assertTrue(ldap.groupSearch().isPresent());
    assertEquals("ou=groups,dc=example,dc=com", ldap.groupSearch().get().baseDn());
  }

  @Test
  void testMigrateLdapWithScopes() throws Exception {
    var sgAuthC = migrate("/xpack_migrate/elasticsearch/auth/ldap_with_scopes.yml");

    assertEquals(1, sgAuthC.authDomains().size());
    var ldap = assertLdapDomain(sgAuthC.authDomains().get(0));

    // SUB_TREE -> SUB
    assertEquals(AuthDomain.Ldap.SearchScope.SUB, ldap.userSearch().get().scope().orElse(null));
    // ONE_LEVEL -> ONE
    assertEquals(AuthDomain.Ldap.SearchScope.ONE, ldap.groupSearch().get().scope().orElse(null));
  }

  @Test
  void testMigrateLdapWithoutGroupSearch() throws Exception {
    var sgAuthC = migrate("/xpack_migrate/elasticsearch/auth/ldap_without_group_search.yml");

    assertEquals(1, sgAuthC.authDomains().size());
    var ldap = assertLdapDomain(sgAuthC.authDomains().get(0));

    assertTrue(ldap.userSearch().isPresent());
    assertFalse(ldap.groupSearch().isPresent());
  }

  @Test
  void testMigrateActiveDirectoryOnly() throws Exception {
    var sgAuthC = migrate("/xpack_migrate/elasticsearch/auth/active_directory_only.yml");

    assertEquals(1, sgAuthC.authDomains().size());
    var ldap = assertLdapDomain(sgAuthC.authDomains().get(0));

    assertTrue(ldap.userSearch().isPresent());
    assertEquals("dc=example,dc=com", ldap.userSearch().get().baseDn().orElse(null));
    assertFalse(ldap.groupSearch().isPresent());
  }

  @Test
  void testMigrateUnsupportedRealm() throws Exception {
    var config = loadConfig("/xpack_migrate/elasticsearch/auth/unsupported_realm.yml");
    var context = new TestMigrationContext(Optional.of(config));
    var migrator = new AuthMigrator();

    assertThrows(UnsupportedOperationException.class, () -> migrator.migrate(context, logger));
  }

  @Test
  void testMigrateEmptyElasticsearchConfig() throws Exception {
    var context = new TestMigrationContext(Optional.empty());
    var migrator = new AuthMigrator();

    List<NamedConfig<?>> result = migrator.migrate(context, logger);

    assertTrue(result.isEmpty());
  }

  @Test
  void testMigrateMultipleRealms() throws Exception {
    var sgAuthC = migrate("/xpack_migrate/elasticsearch/auth/multiple_realms.yml");

    assertEquals(3, sgAuthC.authDomains().size());
  }

  // Helper methods

  private SgAuthC migrate(String path) throws Exception {
    var config = loadConfig(path);
    var context = new TestMigrationContext(Optional.of(config));
    var migrator = new AuthMigrator();

    List<NamedConfig<?>> result = migrator.migrate(context, logger);

    assertEquals(1, result.size());
    var sgAuthC = assertInstanceOf(SgAuthC.class, result.get(0));
    assertEquals("sg_authc.yml", sgAuthC.getFileName());
    return sgAuthC;
  }

  private AuthDomain.Ldap assertLdapDomain(AuthDomain<?> domain) {
    return assertInstanceOf(AuthDomain.Ldap.class, domain);
  }

  private XPackElasticsearchConfig loadConfig(String path) throws Exception {
    var node = readYaml(path);
    return XPackElasticsearchConfig.parse(node, Parser.Context.get());
  }

  private DocNode readYaml(String path) throws IOException, DocumentParseException {
    try (var in = getClass().getResourceAsStream(path)) {
      assertNotNull(in, "Resource not found: " + path);
      return DocNode.wrap(DocReader.yaml().read(in));
    }
  }

  record TestMigrationContext(Optional<XPackElasticsearchConfig> config)
      implements Migrator.IMigrationContext {
    @Override
    public Optional<RoleMappings> getRoleMappings() {
      return Optional.empty();
    }

    @Override
    public Optional<Roles> getRoles() {
      return Optional.empty();
    }

    @Override
    public Optional<Users> getUsers() {
      return Optional.empty();
    }

    @Override
    public Optional<XPackElasticsearchConfig> getElasticsearch() {
      return config;
    }

    @Override
    public Optional<?> getKibana() {
      return Optional.empty();
    }
  }
}
