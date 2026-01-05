package com.floragunn.searchguard.sgctl.util.mapping.ir;

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.GlobalIR;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GlobalIR}.
 */
class GlobalIRTest {

    /**
     * Verifies that the 'enabled' flag is correctly parsed from the global options.
     */
    @Test
    void handleGlobalOptionsShouldSetXpackSecurityEnabled() {
        GlobalIR ir = new GlobalIR();
        File dummyConfig = new File("elasticsearch.yml");
        String prefix = "xpack.security.";

        assertFalse(ir.getXpackSecEnabled(), "Default should be disabled");

        ir.handleGlobalOptions("enabled", Boolean.TRUE, prefix, dummyConfig);

        assertTrue(ir.getXpackSecEnabled(), "X-Pack security should be enabled after setting the flag");
    }

    /**
     * Verifies that unknown keys do not change the internal state.
     */
    @Test
    void handleGlobalOptionsShouldIgnoreUnknownKeysForFlag() {
        GlobalIR ir = new GlobalIR();
        File dummyConfig = new File("elasticsearch.yml");
        String prefix = "xpack.security.";

        ir.handleGlobalOptions("unknown_key", Boolean.TRUE, prefix, dummyConfig);

        assertFalse(ir.getXpackSecEnabled(), "Unknown keys must not change the enabled flag");
    }
}
