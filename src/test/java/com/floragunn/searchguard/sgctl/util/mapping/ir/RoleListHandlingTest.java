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

import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests list immutability and null handling for IR security model classes.
 */
class RoleListHandlingTest {

    /**
     * Verifies field security lists are immutable once set.
     */
    @Test
    void fieldSecurityListsShouldBeImmutable() {
        Role.FieldSecurity fieldSecurity = new Role.FieldSecurity();
        fieldSecurity.setGrant(new ArrayList<>(List.of("field1")));

        assertEquals(List.of("field1"), fieldSecurity.getGrant());
        assertThrows(UnsupportedOperationException.class, () -> fieldSecurity.getGrant().add("field2"));
    }

    /**
     * Verifies empty lists are stored safely and remain immutable.
     */
    @Test
    void fieldSecurityShouldHandleEmptyLists() {
        Role.FieldSecurity fieldSecurity = new Role.FieldSecurity();
        fieldSecurity.setGrant(List.of());

        assertEquals(List.of(), fieldSecurity.getGrant());
        assertThrows(UnsupportedOperationException.class, () -> fieldSecurity.getGrant().add("field1"));
    }

    /**
     * Verifies null field security lists are rejected.
     */
    @Test
    void fieldSecurityShouldRejectNullLists() {
        Role.FieldSecurity fieldSecurity = new Role.FieldSecurity();

        assertThrows(NullPointerException.class, () -> fieldSecurity.setGrant(null));
        assertThrows(NullPointerException.class, () -> fieldSecurity.setExcept(null));
    }

    /**
     * Verifies remote cluster lists are immutable once set.
     */
    @Test
    void remoteClusterListsShouldBeImmutable() {
        Role.RemoteCluster remoteCluster = new Role.RemoteCluster();
        remoteCluster.setClusters(List.of("cluster1"));
        remoteCluster.setPrivileges(List.of("read"));

        assertEquals(List.of("cluster1"), remoteCluster.getClusters());
        assertEquals(List.of("read"), remoteCluster.getPrivileges());
        assertThrows(UnsupportedOperationException.class, () -> remoteCluster.getClusters().add("cluster2"));
        assertThrows(UnsupportedOperationException.class, () -> remoteCluster.getPrivileges().add("write"));
    }

    /**
     * Verifies role cluster permissions are immutable once set.
     */
    @Test
    void roleClusterPermissionsShouldBeImmutable() {
        Role role = new Role("role1");
        role.setCluster(List.of("monitor"));

        assertEquals(List.of("monitor"), role.getCluster());
        assertThrows(UnsupportedOperationException.class, () -> role.getCluster().add("manage"));
    }
}
