package com.floragunn.searchguard.sgctl.config.xpack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RoleTest {

    private static final TypeReference<Map<String, Role>> ROLES_TYPE = new TypeReference<>() {};

    @Test
    public void testRolesJsonRoundTrip() throws IOException {
        final String rolesJson;
        try (var in = RoleTest.class.getResourceAsStream("/xpack_migrate/roles/roles.json")) {
            assertNotNull(in);
            rolesJson = new String(in.readAllBytes());
        }

        final ObjectMapper mapper = JsonMapper.builder()
            .withConfigOverride(List.class, cfg ->
                    cfg.setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
            )
            .serializationInclusion(JsonInclude.Include.NON_EMPTY)
            .build();

        final String roundTripped = mapper.writeValueAsString(mapper.readValue(rolesJson, ROLES_TYPE));
        assertEquals(mapper.readTree(rolesJson), mapper.readTree(roundTripped));
    }

}
