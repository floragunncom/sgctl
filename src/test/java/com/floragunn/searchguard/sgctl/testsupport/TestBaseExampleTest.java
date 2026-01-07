package com.floragunn.searchguard.sgctl.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Simple example test for TestBase to verify resource handling.
 */
class TestBaseExampleTest extends TestBase {

    /**
     * Verifies that a test resource can be resolved to a path and exists.
     */
    @Test
    void shouldResolveResourcePath() {
        Path path = resolveResourcePath("testbase/example.txt");

        assertNotNull(path, "Resolved path must not be null");
        assertTrue(Files.exists(path), "Resolved path should exist");
    }

    /**
     * Verifies that reading a resource as string works and contains expected text.
     */
    @Test
    void shouldReadResourceContent() {
        String content = readResourceAsString("testbase/example.txt");

        assertNotNull(content, "Content must not be null");
        assertTrue(content.contains("Hello TestBase"), "Content should contain marker text");
        String normalized = normalizeLineEndings(content);
        assertEquals(normalized, normalizeLineEndings(content), "Normalization should be stable");
    }

    /**
     * Verifies that readResourceAsString fails for missing resources.
     */
    @Test
    void shouldRejectMissingResourceOnRead() {
        assertThrows(IllegalArgumentException.class, () -> readResourceAsString("missing-resource.txt"));
    }

    /**
     * Verifies line ending normalization for mixed and null inputs.
     */
    @Test
    void shouldNormalizeVariousLineEndings() {
        String mixed = "a\r\nb\rc\n";

        assertEquals("a\nb\nc\n", normalizeLineEndings(mixed));
        assertEquals("", normalizeLineEndings(null));
    }

    /**
     * Verifies that normalization is a no-op for Unix line endings.
     */
    @Test
    void shouldLeaveUnixLineEndingsUntouched() {
        String unix = "a\nb\nc\n";

        assertEquals(unix, normalizeLineEndings(unix));
    }

    /**
     * Verifies that invalid resource names are rejected.
     */
    @Test
    void shouldRejectBlankOrMissingResources() {
        assertThrows(IllegalArgumentException.class, () -> resolveResourcePath(null));
        assertThrows(IllegalArgumentException.class, () -> resolveResourcePath(""));
        assertThrows(IllegalArgumentException.class, () -> resolveResourcePath("   "));
        assertThrows(IllegalArgumentException.class, () -> resolveResourcePath("does-not-exist.txt"));
    }
}
