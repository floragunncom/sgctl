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
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.User;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link UserConfigWriter}.
 */
class UserConfigWriterTest extends QuietTestBase {

    /**
     * Verifies enabled users are written and enriched with attributes.
     */
    @Test
    void shouldWriteOnlyEnabledUsersWithAugmentedAttributes() {
        IntermediateRepresentation ir = new IntermediateRepresentation();

        LinkedHashMap<String, Object> userAttributes = new LinkedHashMap<>();
        userAttributes.put("department", "sales");
        User enabledUser = new User(
                "alice",
                List.of("role1"),
                "Alice Example",
                "alice@example.com",
                true,
                "profile-123",
                userAttributes
        );
        User disabledUser = new User(
                "bob",
                List.of("role2"),
                null,
                null,
                false,
                null,
                new LinkedHashMap<>()
        );

        ir.addUser(enabledUser);
        ir.addUser(disabledUser);

        UserConfigWriter writer = new UserConfigWriter(ir);

        Object basicObject = writer.toBasicObject();
        assertTrue(basicObject instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, UserConfigWriter.SGInternalUser> users =
                (Map<String, UserConfigWriter.SGInternalUser>) basicObject;

        assertEquals(1, users.size());
        assertTrue(users.containsKey("alice"));
        assertFalse(users.containsKey("bob"));

        UserConfigWriter.SGInternalUser sgUser = users.get("alice");
        assertNotNull(sgUser);
        Object userMapObject = sgUser.toBasicObject();
        assertTrue(userMapObject instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) userMapObject;

        assertEquals("Change it", userMap.get("hash"));
        assertEquals(List.of("role1"), userMap.get("search_guard_roles"));
        assertEquals("", userMap.get("description"));

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) userMap.get("attributes");
        assertEquals("sales", attributes.get("department"));
        assertEquals("alice@example.com", attributes.get("email"));
        assertEquals("Alice Example", attributes.get("full_name"));
        assertEquals("profile-123", attributes.get("profile_uid"));
    }

    /**
     * Ensures optional fields are not added when null.
     */
    @Test
    void shouldNotAddOptionalAttributesWhenMissing() {
        IntermediateRepresentation ir = new IntermediateRepresentation();
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("team", "core");
        User user = new User(
                "charlie",
                List.of("roleA"),
                null,
                null,
                true,
                null,
                attributes
        );
        ir.addUser(user);

        UserConfigWriter writer = new UserConfigWriter(ir);

        Object basicObject = writer.toBasicObject();
        @SuppressWarnings("unchecked")
        Map<String, UserConfigWriter.SGInternalUser> users =
                (Map<String, UserConfigWriter.SGInternalUser>) basicObject;
        UserConfigWriter.SGInternalUser sgUser = users.get("charlie");
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) sgUser.toBasicObject();
        @SuppressWarnings("unchecked")
        Map<String, Object> writtenAttributes = (Map<String, Object>) userMap.get("attributes");

        assertEquals("core", writtenAttributes.get("team"));
        assertFalse(writtenAttributes.containsKey("email"));
        assertFalse(writtenAttributes.containsKey("full_name"));
        assertFalse(writtenAttributes.containsKey("profile_uid"));
    }
}
