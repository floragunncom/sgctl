package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.testsupport.QuietTestBase;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        ActionGroupConfigWriter writer = new ActionGroupConfigWriter();
        writer.addActionGroup(ActionGroupConfigWriter.CustomClusterActionGroup.SGS_CANCEL_TASK_CUSTOM);

        Object basicObject = writer.toBasicObject();
        assertTrue(basicObject instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, ActionGroupConfigWriter.ActionGroup> actionGroups =
                (Map<String, ActionGroupConfigWriter.ActionGroup>) basicObject;

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
        ActionGroupConfigWriter writer = new ActionGroupConfigWriter();
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
    void shouldResolveCustomGroupFromPrivilege() {
        ActionGroupConfigWriter.CustomClusterActionGroup group =
                ActionGroupConfigWriter.CustomClusterActionGroup.fromESPrivilege("manage_security");

        assertEquals(ActionGroupConfigWriter.CustomClusterActionGroup.SGS_MANAGE_SECURITY_CUSTOM, group);
    }
}
