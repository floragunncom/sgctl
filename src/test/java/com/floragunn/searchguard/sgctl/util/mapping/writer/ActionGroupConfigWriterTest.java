package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.testsupport.QuietTestBase;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ActionGroupConfigWriter}.
 */
class ActionGroupConfigWriterTest extends QuietTestBase {

    /**
     * Verifies that custom cluster action groups are added and serialized.
     */
    @Test
    void shouldAddCustomActionGroups() {
        IntermediateRepresentation ir = new IntermediateRepresentation();
        ActionGroupConfigWriter writer = new ActionGroupConfigWriter(ir);

        writer.addCustomActionGroups(Set.of(ActionGroupConfigWriter.CustomClusterActionGroup.SGS_CANCEL_TASK_CUSTOM));

        Object basicObject = writer.toBasicObject();
        assertTrue(basicObject instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, ActionGroupConfigWriter.ActionGroup> actionGroups =
                (Map<String, ActionGroupConfigWriter.ActionGroup>) basicObject;

        assertTrue(writer.contains("SGS_CANCEL_TASK_CUSTOM"));
        assertTrue(actionGroups.containsKey("SGS_CANCEL_TASK_CUSTOM"));

        ActionGroupConfigWriter.ActionGroup group = actionGroups.get("SGS_CANCEL_TASK_CUSTOM");
        assertNotNull(group);
        Object groupMapObject = group.toBasicObject();
        assertTrue(groupMapObject instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> groupMap = (Map<String, Object>) groupMapObject;

        assertEquals("cluster", groupMap.get("type"));
        assertTrue(groupMap.containsKey("allowed_actions"));
        assertEquals("Derived from X-Pack Security builtin privilege 'cancel_task'", groupMap.get("description"));
    }

    /**
     * Ensures empty writers have no configured action groups.
     */
    @Test
    void shouldStartWithNoActionGroups() {
        IntermediateRepresentation ir = new IntermediateRepresentation();
        ActionGroupConfigWriter writer = new ActionGroupConfigWriter(ir);

        assertFalse(writer.contains("SGS_CANCEL_TASK_CUSTOM"));
        Object basicObject = writer.toBasicObject();
        @SuppressWarnings("unchecked")
        Map<String, ActionGroupConfigWriter.ActionGroup> actionGroups =
                (Map<String, ActionGroupConfigWriter.ActionGroup>) basicObject;
        assertTrue(actionGroups.isEmpty());
    }

    /**
     * Verifies unknown names resolve to the default custom cluster action group.
     */
    @Test
    void shouldResolveUnknownCustomGroupToDefault() {
        ActionGroupConfigWriter.CustomClusterActionGroup group =
                ActionGroupConfigWriter.CustomClusterActionGroup.from("unknown");

        assertEquals(ActionGroupConfigWriter.CustomClusterActionGroup.SGS_MANAGE_SECURITY_CUSTOM, group);
    }
}
